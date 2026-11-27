
package com.LB.loadbalancer.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.loadbalancer.strategy.LoadBalancingStrategy;

class LoadBalancingStrategyFactoryTest {

    private LoadBalancingStrategy createStrategy(
            LoadBalancingAlgorithm algorithm) {

        LoadBalancingStrategy strategy = mock(LoadBalancingStrategy.class);
        when(strategy.getAlgorithm()).thenReturn(algorithm);
        return strategy;
    }

    private List<LoadBalancingStrategy> createAllStrategies() {
        return List.of(
                createStrategy(LoadBalancingAlgorithm.ROUND_ROBIN),
                createStrategy(LoadBalancingAlgorithm.WEIGHTED_ROUND_ROBIN),
                createStrategy(LoadBalancingAlgorithm.RANDOM),
                createStrategy(LoadBalancingAlgorithm.WEIGHTED_RANDOM),
                createStrategy(LoadBalancingAlgorithm.LEAST_CONNECTIONS),
                createStrategy(LoadBalancingAlgorithm.WEIGHTED_LEAST_CONNECTIONS),
                createStrategy(LoadBalancingAlgorithm.LEAST_RESPONSE_TIME),
                createStrategy(LoadBalancingAlgorithm.POWER_OF_TWO_CHOICES)
        );
    }

    @Test
    void shouldRegisterAllEightStrategies() {
        var strategies = createAllStrategies();

        var factory = new LoadBalancingStrategyFactory(
                strategies, "ROUND_ROBIN");

        for (LoadBalancingAlgorithm algorithm :
                LoadBalancingAlgorithm.values()) {

            assertTrue(factory.isSupported(algorithm));
        }
    }

    @Test
    void shouldUseRoundRobinAsInitialAlgorithm() {
        var factory = new LoadBalancingStrategyFactory(
                createAllStrategies(), "ROUND_ROBIN");

        assertEquals(
                LoadBalancingAlgorithm.ROUND_ROBIN,
                factory.getActiveAlgorithm()
        );
    }

    @Test
    void shouldSwitchAlgorithmAtRuntime() {
        var factory = new LoadBalancingStrategyFactory(
                createAllStrategies(), "ROUND_ROBIN");

        factory.setActiveAlgorithm(
                LoadBalancingAlgorithm.LEAST_CONNECTIONS);

        assertEquals(
                LoadBalancingAlgorithm.LEAST_CONNECTIONS,
                factory.getActiveAlgorithm()
        );
    }

    @Test
    void shouldReturnStrategyForActiveAlgorithm() {
        var strategies = createAllStrategies();

        var factory = new LoadBalancingStrategyFactory(strategies, "ROUND_ROBIN");

        assertSame(strategies.get(0),factory.getStrategy());
    }

    @Test
    void shouldRejectDuplicateStrategyRegistration() {
        var roundRobin = createStrategy(LoadBalancingAlgorithm.ROUND_ROBIN);

        var duplicateRoundRobin = createStrategy(LoadBalancingAlgorithm.ROUND_ROBIN);

        assertThrows(IllegalStateException.class,
                () -> new LoadBalancingStrategyFactory(
                        List.of(roundRobin, duplicateRoundRobin),
                        "ROUND_ROBIN"
                )
        );
    }

    @Test
    void shouldRejectUnsupportedAlgorithmSwitch() {
        var strategies = createAllStrategies();

        // Simulate a factory where RANDOM is not registered
        strategies = strategies.stream()
                .filter(s -> s.getAlgorithm() !=
                        LoadBalancingAlgorithm.RANDOM)
                .toList();

        var factory = new LoadBalancingStrategyFactory(
                strategies, "ROUND_ROBIN");

        assertThrows(
                UnsupportedOperationException.class,
                () -> factory.setActiveAlgorithm(
                        LoadBalancingAlgorithm.RANDOM)
        );
    }

    @Test
    void shouldRejectUnsupportedInitialAlgorithm() {

        var filteredStrategies = createAllStrategies()
                .stream()
                .filter(s -> s.getAlgorithm() !=
                        LoadBalancingAlgorithm.RANDOM)
                .toList();

        assertThrows(
                IllegalStateException.class,
                () -> new LoadBalancingStrategyFactory(
                        filteredStrategies, "RANDOM")
        );
    }
}
