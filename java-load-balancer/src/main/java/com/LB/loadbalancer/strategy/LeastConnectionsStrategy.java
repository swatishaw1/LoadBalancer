package com.LB.loadbalancer.strategy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

@Component
public class LeastConnectionsStrategy implements LoadBalancingStrategy {

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.LEAST_CONNECTIONS;
    }

    @Override
    public Server selectServer(List<Server> servers, List<String> excludedServers) {

        List<Server> availableServers = servers.stream()
                .filter(server -> !excludedServers.contains(server.getUrl()))
                .toList();

        if (availableServers.isEmpty()) {
            throw new RuntimeException("No server is available for load balancing...");
        }

        int minConnections = availableServers.stream()
                .mapToInt(server -> server.getMetrics().getActiveRequests())
                .min()
                .orElseThrow();

        List<Server> leastLoadedServers = availableServers.stream()
                .filter(server -> server.getMetrics().getActiveRequests() == minConnections)
                .toList();

        int index = ThreadLocalRandom.current().nextInt(leastLoadedServers.size());

        return leastLoadedServers.get(index);
    }
}
