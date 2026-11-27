
package com.LB.loadbalancer.strategy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.LB.service.ServerPool;
import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

@Component
public class WeightedRandomStrategy implements LoadBalancingStrategy {

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.WEIGHTED_RANDOM;
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

        long totalWeight = availableServers.stream()
                .mapToLong(Server::getWeight)
                .sum();

        long randomValue = ThreadLocalRandom.current().nextLong(totalWeight);

        long cumulativeWeight = 0;

        for (Server server : availableServers) {
            cumulativeWeight += server.getWeight();

            if (randomValue < cumulativeWeight) {
                return server;
            }
        }

        throw new IllegalStateException("Weighted random selection failed...");
    }
}
