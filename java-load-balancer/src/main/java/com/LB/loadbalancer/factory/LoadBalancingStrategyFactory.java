package com.LB.loadbalancer.factory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.LB.constants.LoadBalancingAlgorithm;
import com.LB.loadbalancer.strategy.LoadBalancingStrategy;
import com.LB.loadbalancer.strategy.RoundRobinStrategy;
import com.LB.loadbalancer.strategy.WeightedRoundRobinStrategy;

@Component
public class LoadBalancingStrategyFactory {

	@Value("${load-balancer.algorithm:ROUND_ROBIN}")
	private String algorithm;

	private RoundRobinStrategy rrStrategy;
	private WeightedRoundRobinStrategy wrrStrategy;

	public LoadBalancingStrategyFactory(RoundRobinStrategy rrStrategy, WeightedRoundRobinStrategy wrrStrategy) {
		this.rrStrategy = rrStrategy;
		this.wrrStrategy = wrrStrategy;
	}

//	//Via application.properties
//	public LoadBalancingStrategy getStrategy() {
//		LoadBalancingAlgorithm algo = LoadBalancingAlgorithm.valueOf(algorithm.toUpperCase());
//
//		return switch (algo) {
//			case ROUND_ROBIN -> rrStrategy;
//			case WEIGHTED_ROUND_ROBIN -> wrrStrategy;
//		};
//	}

	// Returns the configured strategy
	public LoadBalancingStrategy getStrategy() {
		LoadBalancingAlgorithm algo =
				LoadBalancingAlgorithm.valueOf(algorithm.toUpperCase());

		return getStrategy(algo);
	}

	// Returns a strategy based on the requested algorithm
	public LoadBalancingStrategy getStrategy(LoadBalancingAlgorithm algo) {

		return switch (algo) {
			case ROUND_ROBIN -> rrStrategy;
			case WEIGHTED_ROUND_ROBIN -> wrrStrategy;

			default -> throw new UnsupportedOperationException(
					"Algorithm is not implemented yet: " + algo);
		};
	}

}