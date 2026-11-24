package com.LB.loadbalancer.strategy;

import java.util.List;

import com.LB.model.Server;

public interface LoadBalancingStrategy {

	LoadBalancingAlgorithm getAlgorithm();
	
	Server selectServer(List<Server> servers, List<String> excludedServers);
	
}