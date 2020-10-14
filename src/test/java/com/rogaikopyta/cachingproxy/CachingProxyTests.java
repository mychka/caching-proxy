package com.rogaikopyta.cachingproxy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.rogaikopyta.cachingproxy.CachingProxyApp.APP_CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.shadow.com.univocity.parsers.common.ArgumentUtils.EMPTY_STRING_ARRAY;

public class CachingProxyTests {

    private static final String PROXY_PORT = "8080";
    private static final URI PROXY_URI = URI.create("http://localhost:" + PROXY_PORT);

    private static final String BACKEND_ADDR = "http://localhost:8081";

    private static final int REQUESTS_PER_BATCH = 100;

    @Test
    public void testBackendDown() throws IOException, InterruptedException {
        System.setProperty("proxy.port", PROXY_PORT);
        System.setProperty("backend.addr", BACKEND_ADDR);

        CachingProxyApp.main(EMPTY_STRING_ARRAY);

        sendRequestsToProxy();

        TestingBackendController backendController = new TestingBackendController();
        APP_CONTEXT.autowireBean(backendController);
        assertEquals(0, backendController.numOfProcessedRequests);

        backendController.init();
        Thread.sleep(2000);

        assertEquals(REQUESTS_PER_BATCH, backendController.numOfProcessedRequests);

        sendRequestsToProxy();
        Thread.sleep(100);

        assertEquals(REQUESTS_PER_BATCH * 2, backendController.numOfProcessedRequests);
    }

    private static void sendRequestsToProxy() throws InterruptedException {
        HttpRequestTask task = new HttpRequestTask(
                URI.create(String.format("http://localhost:%s/v1.1/measures", PROXY_PORT)),
                "POST",
                Collections.emptyMap(),
                new byte[300]
        );
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < REQUESTS_PER_BATCH; i++) {
            executorService.execute(() -> {
                assertTrue(
                        Utils.sendHttpRequest(PROXY_URI, task)
                );
            });
        }
        executorService.shutdown();
        assertTrue(
                executorService.awaitTermination(1000, TimeUnit.SECONDS)
        );
    }
}
