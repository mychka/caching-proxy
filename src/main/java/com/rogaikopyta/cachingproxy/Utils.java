package com.rogaikopyta.cachingproxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class Utils {

    private Utils() {
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];

        int n;
        while ((n = input.read(buffer)) != -1) {
            bos.write(buffer, 0, n);
        }

        return bos.toByteArray();
    }

    public static boolean sendHttpRequest(URI targetUri, HttpRequestTask request) {
        try {
            URL url;
            try {
                url = new URI(
                        targetUri.getScheme(),
                        targetUri.getAuthority(),
                        request.proxyUri.getPath(),
                        request.proxyUri.getQuery(),
                        request.proxyUri.getFragment()
                ).toURL();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.method);
            for (Map.Entry<String, String> header : request.headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.body);
                return conn.getResponseCode() >= 200 && conn.getResponseCode() < 300;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getPropOrEnv(String name) {
        String value = System.getProperty(name);
        return value == null ? System.getenv(name) : value;
    }

    public static String getPropOrEnvOrDefault(String name, String defaultValue) {
        String value = getPropOrEnv(name);
        return value == null ? defaultValue : value;
    }

    public static int getPropOrEnvOrDefault(String name, int defaultValue) {
        String value = getPropOrEnv(name);
        return value == null ? defaultValue : Integer.parseInt(value);
    }
}
