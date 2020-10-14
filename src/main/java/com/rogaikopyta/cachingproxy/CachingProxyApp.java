package com.rogaikopyta.cachingproxy;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import static com.rogaikopyta.cachingproxy.Utils.getPropOrEnvOrDefault;

public class CachingProxyApp {

    public static final ApplicationContext APP_CONTEXT = new ApplicationContext();

    public static void main(String[] args) throws IOException {
        String backendAddr = getPropOrEnvOrDefault("backend.addr", null);
        if (backendAddr == null) {
            System.err.println("'backend.addr' property is not specified.");
            System.exit(1);
        }

        APP_CONTEXT.registerValue("backend.addr", backendAddr);

        APP_CONTEXT.registerValue("backend.queue", new LinkedBlockingQueue<>());

        APP_CONTEXT.registerBean(
                RetryPolicy.class,
                new ExponentialBackoffRetry(
                        getPropOrEnvOrDefault("backend.initialDelayMillis", 1000),
                        getPropOrEnvOrDefault("backend.numOfRetries", 8)
                )
        );

        registerContextValue("backend.sequentialFailuresThreshold", 7);
        registerContextValue("backend.maxConcurrentRequests", 10);
        registerContextValue("backend.downDelayMillis", 1000);
        registerContextValue("proxy.port", 8080);

        CachingProxyController cachingProxyController = new CachingProxyController();
        APP_CONTEXT.autowireBean(cachingProxyController);
        cachingProxyController.init();

        BackendService backendService = new BackendService();
        APP_CONTEXT.autowireBean(backendService);
        backendService.init();
    }

    private static void registerContextValue(String name, int defaultValue) {
        APP_CONTEXT.registerValue(name, getPropOrEnvOrDefault(name, defaultValue));
    }
}
