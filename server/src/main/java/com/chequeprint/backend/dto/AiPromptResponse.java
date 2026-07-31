package com.chequeprint.backend.dto;

public class AiPromptResponse {

    private boolean success;
    private String response;
    private String error;

    public AiPromptResponse() {}

    public AiPromptResponse(boolean success, String response, String error) {
        this.success = success;
        this.response = response;
        this.error = error;
    }

    public static AiPromptResponse ok(String text) {
        return new AiPromptResponse(true, text, null);
    }

    public static AiPromptResponse fail(String errorMessage) {
        return new AiPromptResponse(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
