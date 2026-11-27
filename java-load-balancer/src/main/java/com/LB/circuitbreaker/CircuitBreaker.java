package com.LB.circuitbreaker;

public class CircuitBreaker {

	public enum States {
		CLOSED,
		OPEN,
		HALF_OPEN
	}

	private States state = States.CLOSED;

	private final int failureThreshold;
	private final long openStateMillis;

	private long openedAt;
	private int failure;

	// Controls the single trial request allowed in HALF_OPEN.
	private boolean halfOpenTrialInProgress = false;

	private final String serverUrl;

	public CircuitBreaker(String serverUrl,int failureThreshold,long openStateMillis) {
		this.serverUrl = serverUrl;
		this.failureThreshold = failureThreshold;
		this.openStateMillis = openStateMillis;
	}

	public synchronized boolean requestAllowed() {

		if (state == States.CLOSED) {
			System.out.println("[CircuitBreaker] " + serverUrl + " CLOSED -> request allowed");
			return true;
		}

		if (state == States.OPEN) {
			long currentOpenDuration = System.currentTimeMillis() - openedAt;

			if (currentOpenDuration < openStateMillis) {
				System.out.println("[CircuitBreaker] " + serverUrl + " OPEN -> request blocked");
				return false;
			}

			state = States.HALF_OPEN;
			halfOpenTrialInProgress = false;

			System.out.println("[CircuitBreaker] " + serverUrl + " OPEN -> HALF_OPEN");
		}

		if (state == States.HALF_OPEN) {
			if (halfOpenTrialInProgress) {
				System.out.println("[CircuitBreaker] " + serverUrl + " HALF_OPEN -> trial blocked");
				return false;
			}

			halfOpenTrialInProgress = true;
			System.out.println("[CircuitBreaker] " + serverUrl + " HALF_OPEN -> trial allowed");
			return true;
		}

		return false;
	}

	public synchronized void recordSuccess() {

		failure = 0;

		if (state == States.HALF_OPEN) {
			state = States.CLOSED;
			halfOpenTrialInProgress = false;
			System.out.println("Server: " + serverUrl + " recovered. HALF_OPEN -> CLOSED...");
		}
	}

	public synchronized void recordFailure() {

		// Trial request failed. Go back to OPEN.
		if (state == States.HALF_OPEN) {
			state = States.OPEN;
			openedAt = System.currentTimeMillis();
			failure = failureThreshold;
			halfOpenTrialInProgress = false;
			System.err.println("Server: " + serverUrl + " trial request failed. HALF_OPEN -> OPEN...");
			return;
		}

		// Don't count failures while already OPEN.
		if (state == States.OPEN) {
			return;
		}

		// CLOSED: count normal failures.
		failure++;
		System.err.println("Server: " + serverUrl + " current failure count is: " + failure);

		if (failure >= failureThreshold) {
			state = States.OPEN;
			openedAt = System.currentTimeMillis();
			System.out.println("Server: " + serverUrl + " state has been updated from CLOSED to OPEN...");
		}
	}

	public synchronized boolean isAvailable() {
		if (state == States.CLOSED) {
			return true;
		}

		if (state == States.OPEN) {
			long currentOpenDuration = System.currentTimeMillis() - openedAt;
			return currentOpenDuration >= openStateMillis;
		}

		if (state == States.HALF_OPEN) {
			return !halfOpenTrialInProgress;
		}

		return false;
	}
}
