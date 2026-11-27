package com.LB.loadbalancer.selector;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.loadbalancer.factory.LoadBalancingStrategyFactory;
import com.LB.model.Server;
import com.LB.service.ServerPool;

@Component
public class AlgorithmMonitor {

    private final int REQUIRED_CONFIRMATIONS;
    private final long COOLDOWN_MS;

    private final ServerPool serverPool;
    private final AlgorithmSelector algorithmSelector;
    private final LoadBalancingStrategyFactory strategyFactory;


    private LoadBalancingAlgorithm candidateAlgorithm;
    private int consecutiveRecommendations;

    private long lastSwitchTime;

    public AlgorithmMonitor(ServerPool serverPool,
                            AlgorithmSelector algorithmSelector,
                            LoadBalancingStrategyFactory strategyFactory,
                            @Value("${algorithm.switching.cooldown.period:300000}") long cooldownPeriod,
                            @Value("${algorithm.switching.threshold:5}") int requiredConfirmations) {

        this.serverPool = serverPool;
        this.algorithmSelector = algorithmSelector;
        this.strategyFactory = strategyFactory;
        this.COOLDOWN_MS = cooldownPeriod;
        this.REQUIRED_CONFIRMATIONS = requiredConfirmations;
    }

    @Scheduled(fixedDelayString = "${load-balancer.algorithm-monitor.interval-ms:5000}")
    public synchronized void evaluateAlgorithm(){

        List<Server> servers = serverPool.getAvailableServers();

        LoadBalancingAlgorithm recommendedAlgorithm = algorithmSelector.selectAlgorithm(servers);

        LoadBalancingAlgorithm activeAlgorithm = strategyFactory.getActiveAlgorithm();

        // Recommendation already matches the active algorithm
        if (recommendedAlgorithm == activeAlgorithm) {
            resetCandidate();
            return;
        }

        // Track how consistently the same algorithm is recommended
        if (recommendedAlgorithm == candidateAlgorithm) {
            consecutiveRecommendations++;
        } else {
            candidateAlgorithm = recommendedAlgorithm;
            consecutiveRecommendations = 1;
        }

        // Require repeated recommendations before switching
        if (consecutiveRecommendations < REQUIRED_CONFIRMATIONS) {
            return;
        }

        // Prevent frequent algorithm switching
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSwitchTime < COOLDOWN_MS) {
            return;
        }

        // Switch to the recommended algorithm
        strategyFactory.setActiveAlgorithm(recommendedAlgorithm);

        lastSwitchTime = currentTime;
        resetCandidate();

        System.out.println("[AlgorithmMonitor] Algorithm switched from " + activeAlgorithm + " to " + recommendedAlgorithm);
    }

    private void resetCandidate() {
        candidateAlgorithm = null;
        consecutiveRecommendations = 0;
    }
}
