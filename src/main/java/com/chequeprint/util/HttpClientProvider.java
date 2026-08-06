package com.chequeprint.util;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared HttpClient provider to prevent resource leaks.
 * HttpClient is thread-safe — one instance serves all DAOs.
 */
public final class HttpClientProvider {

    private HttpClientProvider() {} // utility class, no instances

    private static final HttpClient INSTANCE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static HttpClient getClient() {
        return INSTANCE;
    }
}
