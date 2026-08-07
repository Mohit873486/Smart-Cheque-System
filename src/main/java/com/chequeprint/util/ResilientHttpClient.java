package com.chequeprint.util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resilient HTTP client wrapper with automatic retry.
 *
 * <p>Retry policy:</p>
 * <ul>
 *   <li>3 attempts total (initial + 2 retries)</li>
 *   <li>Back-off delays: 500ms → 1000ms → 1500ms</li>
 *   <li>Retry on: IO errors, timeouts, connection resets, HTTP 5xx, HTTP 429</li>
 *   <li>No retry on: HTTP 4xx (except 429), null requests</li>
 * </ul>
 */
public final class ResilientHttpClient {

    private static final Logger LOGGER = Logger.getLogger(ResilientHttpClient.class.getName());

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long[] DEFAULT_DELAYS_MS = { 500, 1000, 1500 };

    /** HTTP status codes that trigger a retry. */
    private static final List<Integer> RETRYABLE_STATUS_CODES = Arrays.asList(429, 500, 502, 503, 504);

    private ResilientHttpClient() {}

    /**
     * Sends the request with automatic retry using the shared HttpClient.
     *
     * @param request the HTTP request to send
     * @return the HTTP response (last attempt)
     * @throws Exception if all attempts fail
     */
    public static HttpResponse<?> sendWithRetry(HttpRequest request) throws Exception {
        return sendWithRetry(request, HttpClientProvider.getClient(), DEFAULT_MAX_RETRIES, DEFAULT_DELAYS_MS);
    }

    /**
     * Sends the request with custom retry configuration.
     */
    public static HttpResponse<?> sendWithRetry(HttpRequest request, HttpClient client,
                                                int maxRetries, long[] delaysMs) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request must not be null.");
        }

        Exception lastException = null;
        int attempts = Math.max(1, maxRetries);

        for (int attempt = 0; attempt < attempts; attempt++) {
            boolean isRetryAttempt = attempt > 0;

            if (isRetryAttempt) {
                long delay = (attempt - 1 < delaysMs.length) ? delaysMs[attempt - 1] : delaysMs[delaysMs.length - 1];
                // FIX: Lambda hata ke seedha string pass karo
                LOGGER.info(String.format("[ResilientHttpClient] Retry attempt %d/%d for %s %s | waiting %dms",
                        attempt + 1, attempts, request.method(), request.uri(), delay));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted for " + request.method() + " " + request.uri(), ie);
                }
            }

            try {
                HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Retry on 5xx and 429 Too Many Requests
                int status = response.statusCode();
                if (isRetryableStatus(status) && attempt < attempts - 1) {
                    // FIX: Lambda hata ke seedha string pass karo
                    LOGGER.warning(String.format(
                            "[ResilientHttpClient] HTTP %d from %s %s — will retry (%d/%d)",
                            status, request.method(), request.uri(), attempt + 1, attempts));
                    lastException = new IOException("HTTP " + status + " on attempt " + (attempt + 1));
                    continue;
                }

                if (isRetryAttempt) {
                    // FIX: Lambda hata ke seedha string pass karo
                    LOGGER.info(String.format(
                            "[ResilientHttpClient] Success on retry attempt %d for %s %s",
                            attempt + 1, request.method(), request.uri()));
                }
                return response;

            } catch (Exception e) {
                lastException = e;
                if (!isRetryableException(e) || attempt >= attempts - 1) {
                    break;
                }
                // FIX: Lambda hata ke seedha string pass karo
                LOGGER.warning(String.format(
                        "[ResilientHttpClient] Attempt %d/%d failed for %s %s | %s",
                        attempt + 1, attempts, request.method(), request.uri(),
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }

        throw new Exception("[ResilientHttpClient] All " + attempts + " attempts failed for "
                + request.method() + " " + request.uri(), lastException);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Retry classification
    // ═══════════════════════════════════════════════════════════════════════

    private static boolean isRetryableStatus(int statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }

    private static boolean isRetryableException(Exception e) {
        if (e instanceof java.net.http.HttpTimeoutException) return true;
        if (e instanceof java.io.IOException) return true;
        if (e instanceof java.net.ConnectException) return true;
        if (e instanceof java.net.http.HttpConnectTimeoutException) return true;

        String msg = e.getMessage();
        if (msg == null) return false;

        String low = msg.toLowerCase();
        return low.contains("timeout")
            || low.contains("connection refused")
            || low.contains("unreachable")
            || low.contains("reset")
            || low.contains("broken pipe")
            || low.contains("temporary")
            || low.contains("no route to host");
    }
}