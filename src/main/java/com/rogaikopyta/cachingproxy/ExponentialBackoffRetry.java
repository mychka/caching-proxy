package com.rogaikopyta.cachingproxy;

public class ExponentialBackoffRetry implements RetryPolicy {

    private final int initialDelayMillis;

    private final int maxRetries;

    public ExponentialBackoffRetry(int initialDelayMillis, int maxRetries) {
        this.initialDelayMillis = initialDelayMillis;
        this.maxRetries = maxRetries;
    }

    @Override
    public long getRetryDelay(int retryCount, long elapsedMillis) {
        if (retryCount > maxRetries) {
            return -1;
        }

        long sleepMillis = initialDelayMillis * (1 << (retryCount - 1)) - elapsedMillis;

        return sleepMillis < 0 ? 0 : sleepMillis;
    }
}
