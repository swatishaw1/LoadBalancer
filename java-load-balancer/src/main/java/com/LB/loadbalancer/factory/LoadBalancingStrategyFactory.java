package com.LB.loadbalancer.factory;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.loadbalancer.strategy.LoadBalancingStrategy;

@Component
public class LoadBalancingStrategyFactory {

	// Stores each algorithm and its corresponding strategy implementation
	private final Map<LoadBalancingAlgorithm, LoadBalancingStrategy> strategies;

	// Stores the currently active algorithm and allows thread-safe switching
	private final AtomicReference<LoadBalancingAlgorithm> activeAlgorithm;

	public LoadBalancingStrategyFactory(
			List<LoadBalancingStrategy> strategyBeans,
			@Value("${load-balancer.algorithm:ROUND_ROBIN}") String initialAlgorithm) {

		// Creates a map using enum values as keys
		Map<LoadBalancingAlgorithm, LoadBalancingStrategy> registry = new EnumMap<>(LoadBalancingAlgorithm.class);

		// Automatically registers all strategy beans provided by Spring
		for (LoadBalancingStrategy strategy : strategyBeans) {
			LoadBalancingAlgorithm algorithm = strategy.getAlgorithm();

			if (registry.putIfAbsent(algorithm, strategy) != null) {
				throw new IllegalStateException("Duplicate strategy registered for algorithm: " + algorithm);
			}
		}

		// Makes the registry immutable after initialization
		this.strategies = Map.copyOf(registry);

		// Reads and converts the initial algorithm from application.properties
		LoadBalancingAlgorithm initial =
				LoadBalancingAlgorithm.valueOf(initialAlgorithm.trim().toUpperCase(Locale.ROOT));

		if (!strategies.containsKey(initial)) {
			throw new IllegalStateException("Initial algorithm is not implemented: " + initial);
		}

		this.activeAlgorithm = new AtomicReference<>(initial);
	}

	// Returns the strategy of the currently active algorithm
	public LoadBalancingStrategy getStrategy() {
		return getStrategy(activeAlgorithm.get());
	}

	// Returns the strategy for a specific algorithm
	public LoadBalancingStrategy getStrategy(LoadBalancingAlgorithm algorithm) {
		LoadBalancingStrategy strategy = strategies.get(algorithm);

		if (strategy == null) {
			throw new UnsupportedOperationException("Algorithm is not implemented yet: " + algorithm);
		}

		return strategy;
	}

	// Switches the active algorithm at runtime
	public void setActiveAlgorithm(LoadBalancingAlgorithm algorithm) {
		if (!strategies.containsKey(algorithm)) {
			throw new UnsupportedOperationException("Algorithm is not implemented yet: " + algorithm);
		}

		activeAlgorithm.set(algorithm);
	}

	// Returns the currently active algorithm
	public LoadBalancingAlgorithm getActiveAlgorithm() {
		return activeAlgorithm.get();
	}

	// Checks whether a strategy for the given algorithm is registered
	public boolean isSupported(LoadBalancingAlgorithm algorithm) {
		return strategies.containsKey(algorithm);
	}
}