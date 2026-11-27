package com.LB.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.LB.circuitbreaker.CircuitBreaker;
import com.LB.loadbalancer.factory.LoadBalancingStrategyFactory;
import com.LB.loadbalancer.strategy.LoadBalancingStrategy;
import com.LB.model.Server;
import com.LB.model.ServerMetrics;

@Service
public class LoadBalancerService {

	private final int MAX_ATTEMPTS;

	private final LoadBalancingStrategyFactory strategyFactory;
	private final ServerPool serverPool;
	private final RestClient client;

	// Constructor used by Spring in production.
	// Constructor that lets tests supply the HTTP client.
	@Autowired
	LoadBalancerService(LoadBalancingStrategyFactory strategyFactory, ServerPool serverPool,@Value("${load-balancer.max-attempts:2}")  int maxAttempts,
						RestClient client) {
		this.strategyFactory = strategyFactory;
		this.serverPool = serverPool;
		this.MAX_ATTEMPTS = maxAttempts;
		this.client = client;
	}

	public String fwdRequest(String path) {
		List<Server> availableServers = serverPool.getAvailableServers();
		if (availableServers.isEmpty()) {
			throw new RuntimeException("No server is available for load balancing...");
		}

		List<String> attemptedServers = new ArrayList<>();

		LoadBalancingStrategy strategy = strategyFactory.getStrategy();

		int maxAttempts = Math.min(MAX_ATTEMPTS, availableServers.size());

		for (int attempts = 0; attempts < maxAttempts; attempts++){
			Server server = strategy.selectServer(availableServers, attemptedServers);
			attemptedServers.add(server.getUrl());

			System.out.println("Server: " + server.getUrl() + " has been used...");
			CircuitBreaker circuitBreaker = serverPool.getCircuitBreaker(server);

			if(!circuitBreaker.requestAllowed()){
				continue;
			}

			ServerMetrics metrics = server.getMetrics();

			metrics.requestStarted();
			long startTime = System.nanoTime();

			try {
				String response = client.get()
						.uri(server.getUrl() + path)
						.retrieve()
						.body(String.class);

				long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

				metrics.requestSucceeded(responseTimeMs);
				circuitBreaker.recordSuccess();

				return response;

			} catch (Exception ex) {

				long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

				if (ex instanceof HttpClientErrorException clientError && isCallerFault(clientError)) {
					metrics.requestSucceeded(responseTimeMs);
					circuitBreaker.recordSuccess();
					throw clientError;
				}

				metrics.requestFailed(responseTimeMs);

				System.err.println("Server: " + server.getUrl() + " has failed...");
				System.err.println("Exp message: " + ex.getMessage());

				circuitBreaker.recordFailure();
			}
		}

		throw new RuntimeException("Request to all available servers has been failed. Please try after some time...");
	}

	// 408 (Request Timeout) and 429 (Too Many Requests) are 4xx codes, but they
	// say "the server is struggling", so they still count as server failures.
	private boolean isCallerFault(HttpClientErrorException ex) {
		int status = ex.getStatusCode().value();
		return status != 408 && status != 429;
	}

}
