package com.rogaikopyta.cachingproxy;

public interface RetryPolicy {

    /**
     * @return a negative value if an attempt must not be performed anymore,
     *         zero if no delay is required,
     *         otherwise time in millis to wait before the next retry.
     */
    long getRetryDelay(int retryCount, long elapsedMillis);
}
