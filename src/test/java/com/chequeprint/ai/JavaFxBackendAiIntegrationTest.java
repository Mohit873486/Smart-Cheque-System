package com.chequeprint.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JavaFxBackendAiIntegrationTest {

    @Test
    public void testAiServiceEmptyPromptHandling() {
        AiService service = new AiService();
        String response = service.askAI("");
        assertTrue(response.startsWith("Error: Prompt cannot be empty"), "Empty prompt should be rejected before API call.");
    }
}
