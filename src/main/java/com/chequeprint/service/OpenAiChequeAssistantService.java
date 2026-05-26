package com.chequeprint.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class OpenAiChequeAssistantService {

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

    private final OpenAIClient client;

    public OpenAiChequeAssistantService() {
        this(OpenAIOkHttpClient.fromEnv());
    }

    OpenAiChequeAssistantService(OpenAIClient client) {
        this.client = client;
    }

    public String parseCommand(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return emptyAction();
        }

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .instructions(SYSTEM_INSTRUCTIONS)
                .input("Current date: " + java.time.LocalDate.now() + "\nUser input: " + userInput.trim())
                .build();

        Response response = client.responses().create(params);
        return normalizeJson(extractOutputText(response));
    }

    public String runAgent(String userCommand) {
        return parseCommand(userCommand);
    }

    private String extractOutputText(Response response) {
        StringBuilder text = new StringBuilder();

        for (var outputItem : response.output()) {
            if (outputItem.isMessage()) {
                for (var content : outputItem.asMessage().content()) {
                    content.outputText().ifPresent(outputText -> text.append(outputText.text()));
                }
            }
        }

        String result = text.toString().trim();
        return result.isEmpty() ? emptyAction() : result;
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
