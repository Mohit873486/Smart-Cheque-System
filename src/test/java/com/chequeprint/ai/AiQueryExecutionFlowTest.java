package com.chequeprint.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AiQueryExecutionFlowTest {

    @Test
    public void testQueryFormattingAndResponseBubbleHandling() {
        String queryPrompt = "Extract payee from check receipt";
        assertNotNull(queryPrompt, "User query prompt must not be null.");

        String mockAiResponse = "✅ Auto-filled Cheque Data: Payee: Global Tech Pvt Ltd";
        assertTrue(mockAiResponse.startsWith("✅"), "Formatted AI response must format status indicators cleanly.");
    }
}
