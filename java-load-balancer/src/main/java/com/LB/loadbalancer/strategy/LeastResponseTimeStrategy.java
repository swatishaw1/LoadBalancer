package com.LB.loadbalancer.strategy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

@Component
public class LeastResponseTimeStrategy implements LoadBalancingStrategy {

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.LEAST_RESPONSE_TIME;
    }

    @Override
    public Server selectServer(List<Server> servers, List<String> excludedServers) {

        List<Server> availableServers = servers.stream()
                .filter(server -> !excludedServers.contains(server.getUrl()))
                .toList();

        if (availableServers.isEmpty()) {
            throw new RuntimeException("No server is available for load balancing...");
        }

        // Snapshot response times to avoid inconsistent reads during concurrent requests
        List<ServerLatency> latencySnapshot = availableServers.stream()
                .map(server -> new ServerLatency(
                        server,
                        server.getMetrics().getAverageResponseTimeMs()))
                .toList();

        // Find the minimum response time from the snapshot
        long minResponseTime = latencySnapshot.stream()
                .mapToLong(ServerLatency::responseTime)
                .min()
                .orElseThrow();

        // Collect fastest servers using the same snapshot
        List<Server> fastestServers = latencySnapshot.stream()
                .filter(entry -> entry.responseTime() == minResponseTime)
                .map(ServerLatency::server)
                .toList();

        // Randomly break ties
        int index = ThreadLocalRandom.current().nextInt(fastestServers.size());

        return fastestServers.get(index);
    }

    private record ServerLatency(Server server, long responseTime) {}
}