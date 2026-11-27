package com.LB.model;

public class Server {

	private String url;
	private volatile boolean healthy;
	private int weight;
	private int capacity;
	private final ServerMetrics metrics = new ServerMetrics();

	public Server(String url, boolean healthy) {
		this.url = url;
		this.healthy = healthy;
	}

	public Server(String url, boolean healthy, int weight) {
		this(url,healthy);
		this.weight = weight;
	}

	public Server(String url, boolean healthy, int weight, int capacity) {
		this(url,healthy,weight);
		this.capacity = capacity;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public boolean isHealthy() {
		return healthy;
	}

	public void setHealthy(boolean healthy) {
		this.healthy = healthy;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public ServerMetrics getMetrics() {
		return metrics;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public double getUtilization() {
		if (capacity <= 0) {
			return 0.0;
		}

		return (double) metrics.getActiveRequests() / capacity;
	}

}
