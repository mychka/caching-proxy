package com.rogaikopyta.cachingproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static com.rogaikopyta.cachingproxy.HttpRequestTask.POST;
import static com.rogaikopyta.cachingproxy.Utils.toByteArray;

public class CachingProxyController {

    @Value("backend.queue")
    private BlockingQueue<HttpRequestTask> httpRequestQueue;

    @Value("proxy.port")
    private int port;

    public void init() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/v1.1/measures", this::processHttpRequest);
        server.createContext("/v1.1/yetAnotherMeasures", this::processHttpRequest);
        server.start();
    }

    /**
     * Put the incoming request in the queue and respond immediately.
     */
    private void processHttpRequest(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals(POST)) {
            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
                headers.put(
                        header.getKey(),
                        String.join(",", header.getValue())
                );
            }

            httpRequestQueue.add(new HttpRequestTask(
                    exchange.getRequestURI(),
                    POST,
                    headers,
                    toByteArray(exchange.getRequestBody())
            ));

            exchange.sendResponseHeaders(204, 0);
            exchange.close();
        } else {
            exchange.sendResponseHeaders(404, 0);
        }
        exchange.close();
    }
}
