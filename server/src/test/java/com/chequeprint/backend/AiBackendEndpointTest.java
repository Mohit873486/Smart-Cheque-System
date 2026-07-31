package com.chequeprint.backend;

import com.chequeprint.backend.dto.AiPromptRequest;
import com.chequeprint.backend.dto.AiPromptResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AiBackendEndpointTest {

    @Test
    public void testAiPromptRequestAndResponseBinding() {
        AiPromptRequest request = new AiPromptRequest("Extract cheque payee name from text");
        assertEquals("Extract cheque payee name from text", request.getPrompt());

        AiPromptResponse okResponse = AiPromptResponse.ok("Payee: Global Tech");
        assertTrue(okResponse.isSuccess());
        assertEquals("Payee: Global Tech", okResponse.getResponse());
        assertNull(okResponse.getError());

        AiPromptResponse failResponse = AiPromptResponse.fail("Invalid prompt");
        assertFalse(failResponse.isSuccess());
        assertEquals("Invalid prompt", failResponse.getError());
    }
}
