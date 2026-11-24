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

        // Find servers with no successful request history
        List<Server> unmeasuredServers = availableServers.stream()
                .filter(server -> server.getMetrics().getSuccessfulRequests() == 0)
                .toList();

        // Give unmeasured servers a chance to receive requests
        if (!unmeasuredServers.isEmpty()) {
            int index = ThreadLocalRandom.current().nextInt(unmeasuredServers.size());
            return unmeasuredServers.get(index);
        }

        // Find the minimum average response time
        long minResponseTime = availableServers.stream()
                .mapToLong(server -> server.getMetrics().getAverageResponseTimeMs())
                .min()
                .orElseThrow();

        // Collect servers with the minimum response time
        List<Server> fastestServers = availableServers.stream()
                .filter(server -> server.getMetrics().getAverageResponseTimeMs() == minResponseTime)
                .toList();

        // Randomly break ties
        int index = ThreadLocalRandom.current().nextInt(fastestServers.size());

        return fastestServers.get(index);
    }
}