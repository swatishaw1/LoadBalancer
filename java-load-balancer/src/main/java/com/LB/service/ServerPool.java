package com.LB.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.LB.circuitbreaker.CircuitBreaker;
import com.LB.model.Server;

/**
 * Single owner of the backend servers and their circuit breakers. Both
 * LoadBalancerService (to pick a server) and HealthChecker (to update health)
 * read from this one object, so neither needs to know about the other.
 */
@Component
public class ServerPool {

	private final List<Server> servers;
	private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	@Autowired
	public ServerPool(
			@Value("${load-balancer.servers.light-urls:http://localhost:8080,http://localhost:8082}") String lightServerUrls,
			@Value("${load-balancer.servers.heavy-urls:http://localhost:8081,http://localhost:8083}") String heavyServerUrls) {

		this(parseServers(lightServerUrls, 3), parseServers(heavyServerUrls, 1));
	}

	// Constructor for tests
	ServerPool(List<Server> servers) {
		this.servers = List.copyOf(servers);

		for (Server server : servers) {
			circuitBreakers.put(server.getUrl(),new CircuitBreaker(server.getUrl(), 3, 30_000));
		}
	}

	// Constructor for combining Light and Heavy servers
	private ServerPool(List<Server> lightServers, List<Server> heavyServers) {
		this(combineServers(lightServers, heavyServers));
	}

	private static List<Server> parseServers(String urls, int weight) {
		return List.of(urls.split(",")).stream()
				.map(String::trim)
				.filter(url -> !url.isBlank())
				.map(url -> new Server(url, true, weight))
				.toList();
	}

	private static List<Server> combineServers(List<Server> lightServers,List<Server> heavyServers) {

		return java.util.stream.Stream.concat(
				lightServers.stream(),
				heavyServers.stream()
		).toList();
	}

	public List<Server> getServers() {
		return servers;
	}

	public CircuitBreaker getCircuitBreaker(Server server) {
		return circuitBreakers.get(server.getUrl());
	}

	// A server can take traffic only if it is healthy AND its breaker allows it.
	public List<Server> getAvailableServers() {
		return servers.stream()
				.filter(Server::isHealthy)
				.filter(server -> getCircuitBreaker(server).requestAllowed())
				.toList();
	}

}
