package com.chequeprint.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured response from the Cheque API Service
 */
public class ChequeApiResponse {
    private final boolean success;
    private final String message;
    private final List<Map<String, Object>> data;

    public ChequeApiResponse(boolean success, String message, List<Map<String, Object>> data) {
        this.success = success;
        this.message = message;
        this.data = data != null ? data : Collections.emptyList();
    }

    public static ChequeApiResponse success(String message) {
        return new ChequeApiResponse(true, message, null);
    }

    public static ChequeApiResponse success(String message, List<Map<String, Object>> data) {
        return new ChequeApiResponse(true, message, data);
    }

    public static ChequeApiResponse error(String message) {
        return new ChequeApiResponse(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }
}
