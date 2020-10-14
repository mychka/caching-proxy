package com.rogaikopyta.cachingproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import static com.rogaikopyta.cachingproxy.Utils.toByteArray;

public class TestingBackendController {

    @Value("backend.addr")
    private String backendAddr;

    public int numOfProcessedRequests;

    public void init() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(URI.create(backendAddr).getPort()),
                0
        );
        server.createContext("/v1.1/measures", this::processHttpRequest);
        server.createContext("/v1.1/yetAnotherMeasures", this::processHttpRequest);
        server.start();
    }

    private void processHttpRequest(HttpExchange exchange) throws IOException {
        numOfProcessedRequests++;

        // The server must drain the request input stream before sending the response.
        toByteArray(exchange.getRequestBody());

        exchange.sendResponseHeaders(204, 0);
        exchange.close();
    }
}
