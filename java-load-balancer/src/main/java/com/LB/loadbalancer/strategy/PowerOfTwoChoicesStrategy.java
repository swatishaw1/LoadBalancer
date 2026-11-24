package com.LB.loadbalancer.strategy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

@Component
public class PowerOfTwoChoicesStrategy implements LoadBalancingStrategy {

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.POWER_OF_TWO_CHOICES;
    }

    @Override
    public Server selectServer(List<Server> servers, List<String> excludedServers) {

        List<Server> availableServers = servers.stream()
                .filter(server -> !excludedServers.contains(server.getUrl()))
                .toList();

        if (availableServers.isEmpty()) {
            throw new RuntimeException("No server is available for load balancing...");
        }

        if (availableServers.size() == 1) {
            return availableServers.get(0);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Select first candidate
        int firstIndex = random.nextInt(availableServers.size());

        // Select second candidate distinct from the first
        int secondIndex = random.nextInt(availableServers.size() - 1);

        if (secondIndex >= firstIndex) {
            secondIndex++;
        }

        Server first = availableServers.get(firstIndex);
        Server second = availableServers.get(secondIndex);

        int firstLoad = first.getMetrics().getActiveRequests();
        int secondLoad = second.getMetrics().getActiveRequests();

        if (firstLoad < secondLoad) {
            return first;
        }

        if (secondLoad < firstLoad) {
            return second;
        }

        // Random tie-breaker
        return random.nextBoolean() ? first : second;
    }
}
