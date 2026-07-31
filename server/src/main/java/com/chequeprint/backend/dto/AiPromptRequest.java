package com.chequeprint.backend.dto;

public class AiPromptRequest {

    private String prompt;

    public AiPromptRequest() {}

    public AiPromptRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
