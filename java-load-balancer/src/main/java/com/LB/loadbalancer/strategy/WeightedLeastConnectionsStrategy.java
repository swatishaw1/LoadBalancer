package com.LB.loadbalancer.strategy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

@Component
public class WeightedLeastConnectionsStrategy implements LoadBalancingStrategy {

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.WEIGHTED_LEAST_CONNECTIONS;
    }

    @Override
    public Server selectServer(List<Server> servers, List<String> excludedServers) {

        List<Server> availableServers = servers.stream()
                .filter(server -> !excludedServers.contains(server.getUrl()))
                .filter(server -> server.getWeight() > 0)
                .toList();

        if (availableServers.isEmpty()) {
            throw new RuntimeException("No server is available for load balancing...");
        }

        double minScore = availableServers.stream()
                .mapToDouble(server ->
                        (double) server.getMetrics().getActiveRequests() / server.getWeight())
                .min()
                .orElseThrow();

        List<Server> leastLoadedServers = availableServers.stream()
                .filter(server ->
                        (double) server.getMetrics().getActiveRequests() / server.getWeight() == minScore)
                .toList();

        int index = ThreadLocalRandom.current().nextInt(leastLoadedServers.size());

        return leastLoadedServers.get(index);
    }
}
