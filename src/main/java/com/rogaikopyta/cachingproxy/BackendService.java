package com.rogaikopyta.cachingproxy;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static com.rogaikopyta.cachingproxy.Utils.sendHttpRequest;

public class BackendService {

    /**
     * To handle delays between attempts to send requests.
     */
    private final ScheduledExecutorService delayedRequestExecutor = Executors.newSingleThreadScheduledExecutor();

    @Value("backend.queue")
    private BlockingQueue<HttpRequestTask> httpRequestQueue;

    @Value("backend.addr")
    private String backendAddr;

    @Value("backend.downDelayMillis")
    private int downDelayMillis;

    private URI backendUri;

    @Value("backend.sequentialFailuresThreshold")
    private int sequentialFailuresThreshold;

    @Value("backend.maxConcurrentRequests")
    private int maxConcurrentRequests;

    @Inject
    private RetryPolicy retryPolicy;

    private Executor sendRequestExecutor;

    /**
     * Number of requests that are currently being sent to the Backend.
     */
    private int numOfBeingSentRequests;

    private int numOfSequentialFailures;

    /**
     * Timestamp of the last completed (whether successfully or not) request.
     */
    private long lastCompletedAt;

    /**
     * Initially, we don't know a reason of the failure: bad request or the Backend is down. So we temporary put all
     * failed requests to this set, before applying {@link #retryPolicy}.
     */
    private final Set<HttpRequestTask> unknownBackendStatusRequests = new HashSet<>();

    public void init() {
        sendRequestExecutor = Executors.newFixedThreadPool(maxConcurrentRequests);
        backendUri = URI.create(backendAddr);

        new Thread(() -> {
            while (true) {
                try {
                    processHttpRequest(httpRequestQueue.take());
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }).start();
    }

    private synchronized void processHttpRequest(HttpRequestTask request) throws InterruptedException {
        // Apply retry policy.
        if (request.retryCount > 0) {
            long delay = retryPolicy.getRetryDelay(request.retryCount, System.currentTimeMillis() - request.failedAtMillis);
            if (delay < 0) {
                System.err.println("Cannot send request, giving up");
            }
            if (delay > 0) {
                delayedRequestExecutor.schedule(() -> {
                    httpRequestQueue.add(request);
                }, delay, TimeUnit.MILLISECONDS);
            }
            if (delay != 0) {
                return;
            }
        }

        while (true) {
            // If the Backend is down, then we just send a single request per downDelayMillis.
            if (numOfSequentialFailures >= sequentialFailuresThreshold && numOfBeingSentRequests == 0) {
                long delay = downDelayMillis - (System.currentTimeMillis() - lastCompletedAt);
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                executeRequestTask(request);
                break;
            } else if (numOfSequentialFailures < sequentialFailuresThreshold && numOfBeingSentRequests < maxConcurrentRequests) {
                executeRequestTask(request);
                break;
            } else {
                wait();
            }
        }
    }

    private void executeRequestTask(HttpRequestTask request) {
        sendRequestExecutor.execute(() -> {
            onRequestCompleted(request, sendHttpRequest(backendUri, request));
        });
        numOfBeingSentRequests++;
    }

    private synchronized void onRequestCompleted(HttpRequestTask request, boolean success) {
        lastCompletedAt = System.currentTimeMillis();
        numOfBeingSentRequests--;
        if (success) {
            numOfSequentialFailures = 0;
            for (HttpRequestTask unknownBackendStatusRequest : unknownBackendStatusRequests) {
                unknownBackendStatusRequest.retryCount++;
            }
            unknownBackendStatusRequests.clear();
        } else {
            numOfSequentialFailures++;
            if (numOfSequentialFailures >= sequentialFailuresThreshold) {
                unknownBackendStatusRequests.clear();
            } else {
                unknownBackendStatusRequests.add(request);
            }
            httpRequestQueue.add(request);
        }
        notifyAll();
    }
}
