package com.chequeprint.backend;

import com.chequeprint.backend.dto.AiPromptResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AiErrorHandlingTest {

    @Test
    public void testUserFriendlyErrorMessages() {
        AiPromptResponse invalidKey = AiPromptResponse.fail("Invalid AI API key configured. Please verify your Gemini API key settings.");
        assertFalse(invalidKey.isSuccess());
        assertTrue(invalidKey.getError().contains("Invalid AI API key"));

        AiPromptResponse timeout = AiPromptResponse.fail("AI Request Timed Out. Please check your internet connection and try again.");
        assertFalse(timeout.isSuccess());
        assertTrue(timeout.getError().contains("Timed Out"));

        AiPromptResponse connectionErr = AiPromptResponse.fail("Unable to connect to AI service. Please check your internet connection.");
        assertFalse(connectionErr.isSuccess());
        assertTrue(connectionErr.getError().contains("Unable to connect"));
    }
}
