package com.chequeprint.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class OpenAiSqlQueryService {

    private static final String SQL_INSTRUCTIONS = """
            You are an AI that converts natural language into SQL queries.

            Database schema:
            Table: cheques
            Columns:
            - id (integer)
            - cheque_no (text)
            - payee_name (text)
            - amount (number)
            - amount_words (text)
            - bank_id (integer)
            - issue_date (date)
            - status (text)

            Rules:
            - Generate only valid SQL query
            - Do NOT explain anything
            - Only use given table and columns
            - Use SELECT queries only (no DELETE, DROP, UPDATE)
            - If user asks for search, use WHERE condition
            - If user asks "pending", use status = 'Pending'
            - If user asks by name/payee, use payee_name
            - If user asks by date, use issue_date
            - If user asks "history", return all records
            - Limit results to 50 rows
            - Output ONLY SQL query
            """;

    private final OpenAIClient client;

    public OpenAiSqlQueryService() {
        this(OpenAIOkHttpClient.fromEnv());
    }

    OpenAiSqlQueryService(OpenAIClient client) {
        this.client = client;
    }

    public String generateSql(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "SELECT * FROM cheques LIMIT 50;";
        }

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .instructions(SQL_INSTRUCTIONS)
                .input(userInput.trim())
                .build();

        Response response = client.responses().create(params);
        return normalizeSql(extractOutputText(response));
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

        return text.toString().trim();
    }

    private String normalizeSql(String raw) {
        if (raw == null || raw.isBlank()) {
            return "SELECT * FROM cheques LIMIT 50;";
        }

        String sql = raw.trim();
        if (sql.startsWith("```")) {
            sql = sql.replaceFirst("^```(?:sql)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int selectIndex = sql.toUpperCase(java.util.Locale.ROOT).indexOf("SELECT");
        if (selectIndex > 0) {
            sql = sql.substring(selectIndex).trim();
        }

        int semicolon = sql.indexOf(';');
        if (semicolon >= 0) {
            sql = sql.substring(0, semicolon).trim();
        }

        if (!sql.toUpperCase(java.util.Locale.ROOT).contains(" LIMIT ")) {
            sql = sql + " LIMIT 50";
        }

        return sql + ";";
    }
}
