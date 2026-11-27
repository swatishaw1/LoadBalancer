package com.LB.loadbalancer.selector;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

@Component
public class AlgorithmSelector {

    private final double HIGH_UTILIZATION_THRESHOLD;
    private final double IMBALANCE_THRESHOLD;
    private final long HIGH_LATENCY_THRESHOLD_MS;

    public AlgorithmSelector(@Value("${algorithm.high.utilization.threshold:0.70}") double highUtilizationThreshold,
                             @Value("${algorithm.imbalance.threshold:0.25}") double imbalanceThreshold,
                             @Value("${algorithm.high.latency.threshold:500}") long highLatencyThresholdMs) {

        this.HIGH_UTILIZATION_THRESHOLD = highUtilizationThreshold;
        this.IMBALANCE_THRESHOLD = imbalanceThreshold;
        this.HIGH_LATENCY_THRESHOLD_MS = highLatencyThresholdMs;
    }

    public LoadBalancingAlgorithm selectAlgorithm(List<Server> servers) {

        List<Server> healthyServers = servers.stream()
                .filter(Server::isHealthy)
                .filter(server -> server.getCapacity() > 0)
                .toList();

        // No useful servers to analyze
        if (healthyServers.isEmpty()) {
            return LoadBalancingAlgorithm.ROUND_ROBIN;
        }

        // Check whether server response latency is high
        if (hasHighLatency(healthyServers)) {
            return LoadBalancingAlgorithm.LEAST_RESPONSE_TIME;
        }

        // Check overall cluster utilization
        if (isHighUtilization(healthyServers)) {
            return LoadBalancingAlgorithm.POWER_OF_TWO_CHOICES;
        }

        // Check whether load is distributed unevenly
        if (isImbalanced(healthyServers)) {
            if (hasDifferentWeights(healthyServers)) {
                return LoadBalancingAlgorithm.WEIGHTED_LEAST_CONNECTIONS;
            }

            return LoadBalancingAlgorithm.LEAST_CONNECTIONS;
        }

        // Normal load: account for server weights if they differ
        if (hasDifferentWeights(healthyServers)) {
            return LoadBalancingAlgorithm.WEIGHTED_ROUND_ROBIN;
        }

        return LoadBalancingAlgorithm.WEIGHTED_RANDOM;
    }

    private boolean hasHighLatency(List<Server> servers) {

        List<Server> measuredServers = servers.stream()
                .filter(server -> server.getMetrics().getSuccessfulRequests() > 0)
                .toList();

        if (measuredServers.isEmpty()) {
            return false;
        }

        double averageLatency = measuredServers.stream()
                .mapToLong(server -> server.getMetrics().getAverageResponseTimeMs())
                .average()
                .orElse(0.0);

        return averageLatency >= HIGH_LATENCY_THRESHOLD_MS;
    }

    private boolean isHighUtilization(List<Server> servers) {

        long totalActiveRequests = servers.stream()
                .mapToLong(server -> server.getMetrics().getActiveRequests())
                .sum();

        long totalCapacity = servers.stream()
                .mapToLong(Server::getCapacity)
                .sum();

        if (totalCapacity <= 0) {
            return false;
        }

        double utilization = (double) totalActiveRequests / totalCapacity;

        return utilization >= HIGH_UTILIZATION_THRESHOLD;
    }

    private boolean isImbalanced(List<Server> servers) {

        double minUtilization = servers.stream()
                .mapToDouble(Server::getUtilization)
                .min()
                .orElse(0.0);

        double maxUtilization = servers.stream()
                .mapToDouble(Server::getUtilization)
                .max()
                .orElse(0.0);

        return (maxUtilization - minUtilization) >= IMBALANCE_THRESHOLD;
    }

    private boolean hasDifferentWeights(List<Server> servers) {
        int firstWeight = servers.get(0).getWeight();
        return servers.stream().anyMatch(server -> server.getWeight() != firstWeight);
    }
}