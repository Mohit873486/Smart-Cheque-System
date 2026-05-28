package com.chequeprint.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiChequeAssistantService {

    private static final String MODEL = "gemini-1.5";
    private static final String SYSTEM_INSTRUCTIONS = """
            You are an AI assistant for a cheque management system.

            Your job is to convert user input into a structured JSON action.

            Available actions:
            1. PRINT_CHEQUE(name, amount, date)
            2. ADD_CHEQUE(name, amount, date)
            3. SHOW_PENDING_CHEQUES
            4. SHOW_HISTORY
            5. SEARCH_CHEQUE(query)
            6. REMINDER_CHECK

            Rules:
            - Understand user intent correctly
            - Extract name, amount, and date if available
            - Convert relative dates:
              - "today" -> current date
              - "tomorrow" -> next date
            - If any value missing, keep it ""
            - Do NOT explain anything
            - Output ONLY valid JSON

            Output format:
            {
              "action": "",
              "data": {
                "name": "",
                "amount": "",
                "date": "",
                "query": ""
              }
            }
            """;

    private final GeminiApiClient client;

    public OpenAiChequeAssistantService() {
        this(new GeminiApiClient());
    }

    OpenAiChequeAssistantService(GeminiApiClient client) {
        this.client = client;
    }

    public String parseCommand(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return emptyAction();
        }

        List<Map<String, Object>> messages = List.of(
                buildSystemMessage(SYSTEM_INSTRUCTIONS),
                buildUserMessage("Current date: " + java.time.LocalDate.now() + "\nUser input: " + userInput.trim()));

        try {
            String output = client.generateText(MODEL, messages, Map.of(
                    "temperature", 0.0,
                    "max_output_tokens", 256));
            return normalizeJson(output);
        } catch (Exception ex) {
            throw new IllegalStateException("Gemini assistant request failed", ex);
        }
    }

    public String runAgent(String userCommand) {
        return parseCommand(userCommand);
    }

    private Map<String, Object> buildSystemMessage(String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("author", "system");
        message.put("content", List.of(Map.of("type", "text", "text", text)));
        return message;
    }

    private Map<String, Object> buildUserMessage(String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("author", "user");
        message.put("content", List.of(Map.of("type", "text", "text", text)));
        return message;
    }

    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return emptyAction();
        }

        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return emptyAction();
    }

    private String emptyAction() {
        return """
                {"action":"","data":{"name":"","amount":"","date":"","query":""}}
                """.trim();
    }
}
