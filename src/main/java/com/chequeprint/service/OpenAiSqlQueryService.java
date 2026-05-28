package com.chequeprint.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiSqlQueryService {

    private static final String MODEL = "gemini-1.5";
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

    private final GeminiApiClient client;

    public OpenAiSqlQueryService() {
        this(new GeminiApiClient());
    }

    OpenAiSqlQueryService(GeminiApiClient client) {
        this.client = client;
    }

    public String generateSql(String userInput) throws Exception {
        if (userInput == null || userInput.isBlank()) {
            return "SELECT * FROM cheques LIMIT 50;";
        }

        String prompt = SQL_INSTRUCTIONS + "\nUser input: " + userInput.trim();
        String output = client.generateText(MODEL, prompt, 256);
        return normalizeSql(output);
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
