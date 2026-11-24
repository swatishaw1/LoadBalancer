package com.LB.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class ServerMetrics {

    private final AtomicInteger activeRequests = new AtomicInteger(0);

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    private final AtomicLong averageResponseTimeMs = new AtomicLong(0);

    private static final double EMA_ALPHA = 0.2;

    public void requestStarted() {
        activeRequests.incrementAndGet();
        totalRequests.increment();
    }

    public void requestSucceeded(long responseTimeMs) {
        successfulRequests.increment();
        updateAverageResponseTime(responseTimeMs);
        activeRequests.decrementAndGet();
    }

    public void requestFailed(long responseTimeMs) {
        failedRequests.increment();
        updateAverageResponseTime(responseTimeMs);
        activeRequests.decrementAndGet();
    }

    private void updateAverageResponseTime(long responseTimeMs) {
        averageResponseTimeMs.updateAndGet(currentAverage -> {
            if (currentAverage == 0) {
                return responseTimeMs;
            }

            return (long) (EMA_ALPHA * responseTimeMs
                    + (1 - EMA_ALPHA) * currentAverage);
        });
    }

    public int getActiveRequests() {
        return activeRequests.get();
    }

    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.sum();
    }

    public long getFailedRequests() {
        return failedRequests.sum();
    }

    public long getAverageResponseTimeMs() {
        return averageResponseTimeMs.get();
    }
}