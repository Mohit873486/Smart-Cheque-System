package com.chequeprint.service;

import java.util.logging.Logger;

/**
 * Service for interacting with the Google Gemini API.
 * Delegates to GeminiApiClient which uses HttpURLConnection internally.
 */
public class AIService {

    private static final Logger LOGGER = Logger.getLogger(AIService.class.getName());

    // Model as specified by the user
    private static final String MODEL = "gemini-pro";
    private static final int MAX_OUTPUT_TOKENS = 512;

    private final GeminiApiClient geminiClient;

    public AIService() {
        this.geminiClient = new GeminiApiClient();
    }

    /**
     * Sends a prompt to the Gemini AI and retrieves the text response.
     *
     * @param prompt The message to send to the AI
     * @return The AI's response text, or an error message if the call fails
     */
    public String askAI(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Error: Prompt cannot be empty.";
        }

        try {
            // Delegate to GeminiApiClient (uses HttpURLConnection + Jackson)
            return geminiClient.generateText(MODEL, prompt, MAX_OUTPUT_TOKENS);
        } catch (Exception e) {
            LOGGER.severe("AI API Error: " + e.getMessage());

            String message = e.getMessage();
            if (message != null && message.contains("GEMINI_API_KEY")) {
                return "Error: Gemini API key is not configured.\n\nPlease set the GEMINI_API_KEY environment variable and restart the application.";
            }
            if (message != null && message.contains("HTTP 429")) {
                return "Error: API rate limit exceeded. Please wait a moment and try again.";
            }
            if (message != null && (message.contains("HTTP 401") || message.contains("HTTP 400"))) {
                return "Error: Invalid or missing API key. Please check your GEMINI_API_KEY environment variable.";
            }
            return "Error: Unable to connect to the AI API. " + e.getMessage();
        }
    }

    /**
     * Extracts structured cheque data (JSON) from raw OCR text using Gemini.
     */
    public String extractChequeData(String ocrText) {
        String prompt = "Extract the cheque details from the following text. "
                + "You MUST return ONLY a valid JSON object with the keys: "
                + "'payee', 'amount_number', 'amount_words', 'date'. "
                + "Do not include markdown blocks, just the raw JSON string.\nText:\n"
                + ocrText;
        return askAI(prompt);
    }
}
