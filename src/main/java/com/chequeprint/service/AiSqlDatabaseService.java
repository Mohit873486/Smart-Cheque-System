package com.chequeprint.service;

import com.chequeprint.config.AppConfig;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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

        try (Statement statement = AppConfig.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            ResultSetMetaData meta = resultSet.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();

            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), resultSet.getObject(i));
                }
                rows.add(row);
            }

            return rows;
        }
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
