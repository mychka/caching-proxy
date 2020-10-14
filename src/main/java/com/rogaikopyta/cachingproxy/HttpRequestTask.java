package com.rogaikopyta.cachingproxy;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class HttpRequestTask {

    public static final String POST = "POST";

    public final URI proxyUri;

    public final String method;

    public final Map<String, String> headers;

    public final byte[] body;

    int retryCount;

    long failedAtMillis;

    public HttpRequestTask(URI proxyUri, String method, Map<String, String> headers, byte[] body) {
        this.proxyUri = proxyUri;
        this.method = method;
        this.headers = Collections.unmodifiableMap(headers);
        this.body = body;
    }
}
