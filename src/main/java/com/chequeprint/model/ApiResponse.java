package com.chequeprint.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private long timestamp;
    private int httpStatus;

    public ApiResponse() {}

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        r.httpStatus = 200;
        return r;
    }

    public static <T> ApiResponse<T> error(String message, int httpStatus) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.message = message;
        r.httpStatus = httpStatus;
        return r;
    }

    public boolean isOk() {
        return success && httpStatus >= 200 && httpStatus < 300;
    }

    public T orElseThrow() {
        if (!isOk()) {
            throw new RuntimeException(message != null ? message : "API request failed with status " + httpStatus);
        }
        return data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
}