package com.chequeprint.service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.chequeprint.config.ApiConfig;

public class AiSqlDatabaseService {

    private static final Pattern SELECT_ONLY = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALLOWED_TABLE = Pattern.compile("\\bFROM\\s+cheques\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_50 = Pattern.compile("\\bLIMIT\\s+50\\b", Pattern.CASE_INSENSITIVE);

    private final OpenAiSqlQueryService sqlQueryService;

    public AiSqlDatabaseService() {
        this(new OpenAiSqlQueryService());
    }

    AiSqlDatabaseService(OpenAiSqlQueryService sqlQueryService) {
        this.sqlQueryService = sqlQueryService;
    }

    public SqlResult askDatabase(String userInput) throws Exception {
        String sql = sqlQueryService.generateSql(userInput);
        validateSelectSql(sql);
        return new SqlResult(sql, executeSelect(sql));
    }

    public List<Map<String, Object>> executeSelect(String sql) throws Exception {
        validateSelectSql(sql);

        // Client cannot execute SQL directly — send to backend API
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ApiConfig.BASE_URL + "/api/ai/sql"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", com.chequeprint.util.Session.getAuthorizationHeader())
                    .POST(java.net.http.HttpRequest.BodyPublishers
                            .ofString("{\"sql\":\"" + sql.replace("\"", "\\\"") + "\"}"))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                        });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL via API: " + e.getMessage(), e);
        }
        return List.of();
    }

    private void validateSelectSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query is empty.");
        }

        String normalized = sql.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);

        if (!SELECT_ONLY.matcher(normalized).find()) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }
        if (upper.contains("DELETE")
                || upper.contains("DROP")
                || upper.contains("UPDATE")
                || upper.contains("INSERT")
                || upper.contains("ALTER")
                || upper.contains("TRUNCATE")
                || upper.contains("CREATE")) {
            throw new IllegalArgumentException("Unsafe SQL keyword blocked.");
        }
        if (normalized.indexOf(';') != normalized.length() - 1) {
            throw new IllegalArgumentException("Only one SQL statement is allowed.");
        }
        if (!ALLOWED_TABLE.matcher(normalized).find()) {
            throw new IllegalArgumentException("Only table 'cheques' is allowed.");
        }
        if (!LIMIT_50.matcher(normalized).find()) {
            throw new IllegalArgumentException("Query must include LIMIT 50.");
        }
    }

    public static class SqlResult {
        private final String sql;
        private final List<Map<String, Object>> rows;

        public SqlResult(String sql, List<Map<String, Object>> rows) {
            this.sql = sql;
            this.rows = rows == null ? List.of() : rows;
        }

        public String getSql() {
            return sql;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }
    }
}
