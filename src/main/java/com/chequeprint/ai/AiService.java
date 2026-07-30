package com.chequeprint.ai;

import com.chequeprint.service.GeminiApiClient;
import java.util.logging.Logger;

/**
 * Service for interacting with the AI and Google Gemini API.
 */
public class AiService {

    private static final Logger LOGGER = Logger.getLogger(AiService.class.getName());
    private static final String MODEL = "gemini-pro";
    private static final int MAX_OUTPUT_TOKENS = 512;
    private static final AiService INSTANCE = new AiService();

    private final GeminiApiClient geminiClient;

    public AiService() {
        this.geminiClient = new GeminiApiClient();
    }

    public static AiService getInstance() {
        return INSTANCE;
    }

    public String askAI(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Error: Prompt cannot be empty.";
        }

        try {
            return geminiClient.generateText(MODEL, prompt, MAX_OUTPUT_TOKENS);
        } catch (Exception e) {
            LOGGER.severe("AI API Error: " + e.getMessage());
            String message = e.getMessage();
            if (message != null && message.contains("GEMINI_API_KEY")) {
                return "Error: GEMINI_API_KEY environment variable is not set. Please configure it to use AI features.";
            }
            return "Error communicating with AI service: " + (message != null ? message : "Unknown error");
        }
    }

    public String extractChequeData(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        return askAI("Extract cheque information (Payee, Amount, Date) from: " + text);
    }
}
