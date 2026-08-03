package com.chequeprint.api;

import java.util.Objects;
import java.util.Optional;

public final class ApiResponse<T> {
    private final int statusCode;
    private final T body;
    private final String rawBody;
    private final String errorMessage;

    private ApiResponse(int statusCode, T body, String rawBody, String errorMessage) {
        this.statusCode = statusCode;
        this.body = body;
        this.rawBody = rawBody == null ? "" : rawBody;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static <T> ApiResponse<T> success(int statusCode, T body, String rawBody) {
        return new ApiResponse<>(statusCode, body, rawBody, "");
    }

    public static <T> ApiResponse<T> failure(int statusCode, String rawBody, String errorMessage) {
        return new ApiResponse<>(statusCode, null, rawBody, errorMessage);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Optional<T> getBody() {
        return Optional.ofNullable(body);
    }

    public T requireBody(String message) {
        return getBody().orElseThrow(() -> new IllegalStateException(message));
    }

    public String getRawBody() {
        return rawBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public void throwIfFailed(String operation) {
        if (!isSuccessful()) {
            String details = errorMessage.isBlank() ? rawBody : errorMessage;
            throw new IllegalStateException(Objects.requireNonNullElse(operation, "API request")
                    + " failed. HTTP Status: " + statusCode
                    + (details.isBlank() ? "" : " - " + details));
        }
    }
}
