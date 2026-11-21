//package com.LB.circuitbreaker;
//
//public class CircuitBreaker {
//
//	public enum States {
//		OPEN, CLOSED;
//	}
//
//	private States state = States.CLOSED;
//	private final int failureThreshold;
//	private final long openStateMillis;
//	private long openedAt;
//	private int failure;
//	private final String serverUrl;
//
//	public CircuitBreaker(String serverUrl, int failureThreshold, long openStateMillis) {
//		this.serverUrl = serverUrl;
//		this.failureThreshold = failureThreshold;
//		this.openStateMillis = openStateMillis;
//	}
//
//
//	public synchronized boolean requestAllowed() {
//		if(state == States.CLOSED) {
//			return true;
//		}
//
//		//OPEN state
//		long currentOpenDuration = System.currentTimeMillis() - openedAt;
//		if(currentOpenDuration >= openStateMillis) {
//			state = States.CLOSED;
//			failure = 0;
//
//			System.out.println("Server: " + serverUrl + " state has been updated from OPEN to CLOSED...");
//			return true;
//		}
//
//		return false;
//	}
//
//	public synchronized void recordSuccess() {
//		failure = 0;
//	}
//
//	public synchronized void recordFailure() {
//		failure++;
//		System.err.println("Server: " + serverUrl + " current failure count is: " + failure);
//
//		if(failure >= failureThreshold) {
//			state = States.OPEN;
//			openedAt = System.currentTimeMillis();
//
//			System.out.println("Server: " + serverUrl + " state has been updated from CLOSED to OPEN...");
//		}
//	}
//
//}




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

	public CircuitBreaker(
			String serverUrl,
			int failureThreshold,
			long openStateMillis) {

		this.serverUrl = serverUrl;
		this.failureThreshold = failureThreshold;
		this.openStateMillis = openStateMillis;
	}

	public synchronized boolean requestAllowed() {

		// CLOSED: allow normal traffic.
		if (state == States.CLOSED) {
			return true;
		}

		// OPEN: block traffic until cooldown expires.
		if (state == States.OPEN) {

			long currentOpenDuration =
					System.currentTimeMillis() - openedAt;

			if (currentOpenDuration < openStateMillis) {
				return false;
			}

			// Cooldown expired. Move to HALF_OPEN.
			state = States.HALF_OPEN;
			halfOpenTrialInProgress = false;

			System.out.println(
					"Server: " + serverUrl
							+ " state has been updated from OPEN to HALF_OPEN...");
		}

		// HALF_OPEN: allow only one trial request.
		if (state == States.HALF_OPEN) {

			if (halfOpenTrialInProgress) {
				return false;
			}

			halfOpenTrialInProgress = true;

			System.out.println(
					"Server: " + serverUrl
							+ " is allowing a trial request...");

			return true;
		}

		return false;
	}

	public synchronized void recordSuccess() {

		failure = 0;

		if (state == States.HALF_OPEN) {

			state = States.CLOSED;
			halfOpenTrialInProgress = false;

			System.out.println(
					"Server: " + serverUrl
							+ " recovered. HALF_OPEN -> CLOSED...");
		}
	}

	public synchronized void recordFailure() {

		// Trial request failed. Go back to OPEN.
		if (state == States.HALF_OPEN) {

			state = States.OPEN;
			openedAt = System.currentTimeMillis();
			failure = failureThreshold;

			halfOpenTrialInProgress = false;

			System.err.println(
					"Server: " + serverUrl
							+ " trial request failed. HALF_OPEN -> OPEN...");

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

			System.out.println(
					"Server: " + serverUrl
							+ " state has been updated from CLOSED to OPEN...");
		}
	}
}
