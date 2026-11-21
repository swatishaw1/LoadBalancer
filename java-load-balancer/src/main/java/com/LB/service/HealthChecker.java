package com.LB.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.LB.model.Server;

@Component
public class HealthChecker {

	private final RestClient client;
	private final ServerPool serverPool;

	public HealthChecker(ServerPool serverPool, RestClient client) {
		this.serverPool = serverPool;
		this.client = client;
	}

	public boolean isHelathy(Server server) {
		System.out.println("Server: " + server.getUrl() + " getting called...");
		try {
			client.get().uri(server.getUrl() + "/health").retrieve().toBodilessEntity();
			return true;
		} catch (Exception ex) {
			System.err.println("Server: " + server.getUrl() + " is down...");
			return false;
		}
	}

	@Scheduled(fixedRateString = "${load-balancer.health-check-interval-ms:60000}")
	public void updateServerHealth() {
		for (Server server : serverPool.getServers()) {
			boolean isHealthy = isHelathy(server);

			server.setHealthy(isHealthy);
			System.out.println("Updating the server: " + server.getUrl() + " is " + isHealthy);
		}
	}

}
