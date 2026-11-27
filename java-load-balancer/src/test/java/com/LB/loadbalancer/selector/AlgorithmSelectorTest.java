package com.LB.loadbalancer.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;

class AlgorithmSelectorTest {

    private final AlgorithmSelector selector =
            new AlgorithmSelector(0.70, 0.25, 500);

    @Test
    void shouldReturnRoundRobinWhenNoServersAvailable() {
        assertEquals(
                LoadBalancingAlgorithm.ROUND_ROBIN,
                selector.selectAlgorithm(List.of())
        );
    }

    @Test
    void shouldChooseWeightedRoundRobinForNormalLoadWithDifferentWeights() {
        List<Server> servers = List.of(
                new Server("light-1", true, 3, 300),
                new Server("heavy-1", true, 1, 100)
        );

        assertEquals(
                LoadBalancingAlgorithm.WEIGHTED_ROUND_ROBIN,
                selector.selectAlgorithm(servers)
        );
    }

    @Test
    void shouldChooseLeastResponseTimeWhenLatencyIsHigh() {
        Server light = new Server("light-1", true, 3, 300);
        Server heavy = new Server("heavy-1", true, 1, 100);

        light.getMetrics().requestStarted();
        light.getMetrics().requestSucceeded(600);

        heavy.getMetrics().requestStarted();
        heavy.getMetrics().requestSucceeded(600);

        assertEquals(
                LoadBalancingAlgorithm.LEAST_RESPONSE_TIME,
                selector.selectAlgorithm(List.of(light, heavy))
        );
    }

    @Test
    void shouldChoosePowerOfTwoChoicesWhenUtilizationIsHigh() {
        Server light = new Server("light-1", true, 3, 300);
        Server heavy = new Server("heavy-1", true, 1, 100);

        // 240/300 = 80% utilization
        addActiveRequests(light, 240);

        // 80/100 = 80% utilization
        addActiveRequests(heavy, 80);

        assertEquals(
                LoadBalancingAlgorithm.POWER_OF_TWO_CHOICES,
                selector.selectAlgorithm(List.of(light, heavy))
        );
    }

    @Test
    void shouldChooseWeightedLeastConnectionsWhenLoadIsImbalanced() {
        Server light = new Server("light-1", true, 3, 300);
        Server heavy = new Server("heavy-1", true, 1, 100);

        // Light: 30% utilization
        addActiveRequests(light, 90);

        // Heavy: 60% utilization
        addActiveRequests(heavy, 60);

        assertEquals(
                LoadBalancingAlgorithm.WEIGHTED_LEAST_CONNECTIONS,
                selector.selectAlgorithm(List.of(light, heavy))
        );
    }

    @Test
    void shouldChooseLeastConnectionsWhenSameWeightServersAreImbalanced() {
        Server server1 = new Server("server-1", true, 1, 100);
        Server server2 = new Server("server-2", true, 1, 100);

        // Server 1: 40% utilization
        addActiveRequests(server1, 40);

        // Server 2: 0% utilization
        // Significant imbalance exists

        assertEquals(
                LoadBalancingAlgorithm.LEAST_CONNECTIONS,
                selector.selectAlgorithm(List.of(server1, server2))
        );
    }

    @Test
    void shouldIgnoreUnhealthyServers() {
        Server unhealthy = new Server("server-1", false, 1, 100);
        Server healthy = new Server("server-2", true, 1, 100);

        addActiveRequests(unhealthy, 100);

        assertEquals(
                LoadBalancingAlgorithm.WEIGHTED_RANDOM,
                selector.selectAlgorithm(List.of(unhealthy, healthy))
        );
    }

    private void addActiveRequests(Server server, int count) {
        for (int i = 0; i < count; i++) {
            server.getMetrics().requestStarted();
        }
    }
}
