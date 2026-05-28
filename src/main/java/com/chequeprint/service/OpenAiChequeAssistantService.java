package com.chequeprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class OpenAiChequeAssistantService {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String SYSTEM_INSTRUCTIONS = """
            You are a cheque management assistant.

            Convert the user's natural language command into EXACT JSON only.
            Do not add any explanation, markdown, or extra text.
            If the user asks to add a cheque, set action to ADD_CHEQUE.
            If the user asks for pending cheques, set action to SHOW_PENDING_CHEQUES.
            If the user asks for cheque history, set action to SHOW_HISTORY.
            If the user asks to print a cheque, set action to PRINT_CHEQUE.
            If the user asks to search cheques, set action to SEARCH_CHEQUE.
            If the user asks for reminders, set action to REMINDER_CHECK.

            Output MUST be a single JSON object with these fields:
            {
              "action": "",
              "name": "",
              "amount": 0,
              "date": ""
            }

            Rules:
            - Always return only valid JSON.
            - Do not wrap the result in additional fields.
            - Do not include any comments or notes.
            - Use 0 for amount when no numeric amount is present.
            - Use empty string for name and date when unknown.
            - Keep date values as plain text: "today", "tomorrow", or ISO yyyy-MM-dd.
            - If the command is an add cheque request, extract name, amount, and date.
            - If the command is not add-cheque, set other fields to default values.
            """;

    private final GeminiApiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiChequeAssistantService() {
        this(new GeminiApiClient());
    }

    OpenAiChequeAssistantService(GeminiApiClient client) {
        this.client = client;
    }

    public String parseCommandJson(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return emptyJson();
        }

        String prompt = SYSTEM_INSTRUCTIONS + "\nUser input: " + userInput.trim();
        try {
            String output = client.generateText(MODEL, prompt, 256);
            return normalizeJson(output);
        } catch (Exception ex) {
            throw new IllegalStateException("Gemini assistant request failed", ex);
        }
    }

    public String runAgent(String userInput) {
        return parseCommandJson(userInput);
    }

    public ChequeCommand parseCommand(String userInput) {
        String json = parseCommandJson(userInput);
        try {
            return mapper.readValue(json, ChequeCommand.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Gemini JSON response", ex);
        }
    }

    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return emptyJson();
        }

        String cleaned = raw.trim();
        if (cleaned.startsWith("```") || cleaned.startsWith("``json")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }

        return emptyJson();
    }

    private String emptyJson() {
        return "{\"action\":\"\",\"name\":\"\",\"amount\":0,\"date\":\"\"}";
    }

    public static class ChequeCommand {
        private String action = "";
        private String name = "";
        private int amount;
        private String date = "";

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}
