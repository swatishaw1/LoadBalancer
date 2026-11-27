
package com.LB.loadbalancer.selector;

import static org.mockito.Mockito.*;

import java.util.List;

import com.LB.loadbalancer.factory.LoadBalancingStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.model.Server;
import com.LB.service.ServerPool;

class AlgorithmMonitorTest {

    private ServerPool serverPool;
    private AlgorithmSelector algorithmSelector;
    private LoadBalancingStrategyFactory strategyFactory;

    private AlgorithmMonitor monitor;

    private List<Server> servers;

    @BeforeEach
    void setUp() {
        serverPool = mock(ServerPool.class);
        algorithmSelector = mock(AlgorithmSelector.class);
        strategyFactory = mock(LoadBalancingStrategyFactory.class);

        servers = List.of(mock(Server.class));

        when(serverPool.getAvailableServers()).thenReturn(servers);
        when(strategyFactory.getActiveAlgorithm())
                .thenReturn(LoadBalancingAlgorithm.ROUND_ROBIN);

        monitor = new AlgorithmMonitor(
                serverPool,
                algorithmSelector,
                strategyFactory,
                30_000,
                3
        );
    }

    @Test
    void shouldNotSwitchAfterOneRecommendation() {
        when(algorithmSelector.selectAlgorithm(servers))
                .thenReturn(LoadBalancingAlgorithm.LEAST_CONNECTIONS);

        monitor.evaluateAlgorithm();

        verify(strategyFactory, never())
                .setActiveAlgorithm(any());
    }

    @Test
    void shouldNotSwitchAfterTwoRecommendations() {
        when(algorithmSelector.selectAlgorithm(servers))
                .thenReturn(LoadBalancingAlgorithm.LEAST_CONNECTIONS);

        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();

        verify(strategyFactory, never())
                .setActiveAlgorithm(any());
    }

    @Test
    void shouldSwitchAfterThreeConsecutiveRecommendations() {
        when(algorithmSelector.selectAlgorithm(servers))
                .thenReturn(LoadBalancingAlgorithm.LEAST_CONNECTIONS);

        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();

        verify(strategyFactory, times(1))
                .setActiveAlgorithm(
                        LoadBalancingAlgorithm.LEAST_CONNECTIONS);
    }

    @Test
    void shouldResetConfirmationCountWhenRecommendationChanges() {
        when(algorithmSelector.selectAlgorithm(servers))
                .thenReturn(
                        LoadBalancingAlgorithm.LEAST_CONNECTIONS,
                        LoadBalancingAlgorithm.LEAST_CONNECTIONS,
                        LoadBalancingAlgorithm.ROUND_ROBIN,
                        LoadBalancingAlgorithm.LEAST_CONNECTIONS,
                        LoadBalancingAlgorithm.LEAST_CONNECTIONS
                );

        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();

        verify(strategyFactory, never())
                .setActiveAlgorithm(any());
    }

    @Test
    void shouldNotSwitchWhenRecommendedAlgorithmIsAlreadyActive() {
        when(algorithmSelector.selectAlgorithm(servers))
                .thenReturn(LoadBalancingAlgorithm.ROUND_ROBIN);

        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();

        verify(strategyFactory, never())
                .setActiveAlgorithm(any());
    }

    @Test
    void shouldNotSwitchDuringCooldown() throws Exception {
        when(algorithmSelector.selectAlgorithm(servers))
                .thenReturn(LoadBalancingAlgorithm.LEAST_CONNECTIONS);

        // Simulate a recent algorithm switch
        var field = AlgorithmMonitor.class.getDeclaredField("lastSwitchTime");
        field.setAccessible(true);
        field.setLong(monitor, System.currentTimeMillis());

        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();
        monitor.evaluateAlgorithm();

        verify(strategyFactory, never())
                .setActiveAlgorithm(any());
    }
}
