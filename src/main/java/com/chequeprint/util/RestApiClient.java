package com.chequeprint.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public final class RestApiClient {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final AtomicLong callCounter = new AtomicLong(0);

    private RestApiClient() {}

    public static HttpRequest.Builder requestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json");
        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public static HttpResponse<String> send(HttpRequest request) throws Exception {
        long count = callCounter.incrementAndGet();
        long startTime = System.currentTimeMillis();
        String caller = getCallerMethodInfo();

        System.out.println(String.format("API_CALL_START | %s %s | Call #%d | Caller: %s | Time=%d",
                request.method(), request.uri(), count, caller, startTime));

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;
            System.out.println(String.format("API_CALL_END | %s %s | Status=%d | Duration=%dms | Time=%d",
                    request.method(), request.uri(), response.statusCode(), duration, System.currentTimeMillis()));
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println(String.format("API_CALL_END | %s %s | ERROR=%s | Duration=%dms | Time=%d",
                    request.method(), request.uri(), e.getMessage(), duration, System.currentTimeMillis()));
            throw e;
        }
    }

    private static String getCallerMethodInfo() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!className.contains("RestApiClient") && !className.contains("java.lang.Thread")) {
                return stack[i].getFileName() + ":" + stack[i].getLineNumber() + " (" + stack[i].getMethodName() + ")";
            }
        }
        return "UnknownCaller";
    }
}
