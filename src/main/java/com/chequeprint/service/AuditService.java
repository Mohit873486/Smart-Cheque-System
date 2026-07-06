package com.chequeprint.service;

import com.chequeprint.model.AuditAction;
import com.chequeprint.model.AuditLog;
import com.chequeprint.model.User;
import com.chequeprint.util.SessionManager;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditService {

  private static final String BASE_URL = "http://localhost:8081/api/audit";
  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final ObjectMapper objectMapper;

  public AuditService() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    SimpleModule module = new SimpleModule();
    module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(value.toString());
            } else {
                gen.writeNull();
            }
        }
    });
    module.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String val = p.getValueAsString();
            return (val == null || val.isBlank()) ? null : LocalDateTime.parse(val);
        }
    });
    this.objectMapper.registerModule(module);
  }

  private HttpRequest.Builder requestBuilder(String url) {
    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
    String token = SessionManager.getJwtToken();
    if (token != null && !token.isBlank()) {
      builder.header("Authorization", "Bearer " + token);
    }
    return builder;
  }

  public void record(User user, String tableName, Integer recordId, AuditAction action, String details)
      throws SQLException {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("userId", user == null ? null : user.getId());
      payload.put("tableName", tableName);
      payload.put("recordId", recordId);
      payload.put("action", action.name());
      payload.put("details", details);

      String json = objectMapper.writeValueAsString(payload);

      HttpRequest request = requestBuilder(BASE_URL)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 201) {
        throw new IOException("Failed to record audit log. HTTP: " + response.statusCode());
      }
    } catch (Exception e) {
      System.err.println("REST API audit log record failure: " + e.getMessage());
    }
  }

  public void recordLogin(User user) throws SQLException {
    record(user, "users", user.getId(), AuditAction.LOGIN, "User logged in.");
  }

  public void recordLogout(User user) throws SQLException {
    record(user, "users", user.getId(), AuditAction.LOGOUT, "User logged out.");
  }

  public List<AuditLog> findRecent(int limit) throws SQLException {
    try {
      HttpRequest request = requestBuilder(BASE_URL + "/recent?limit=" + limit)
              .GET()
              .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return objectMapper.readValue(response.body(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, AuditLog.class));
      } else {
        throw new IOException("Failed to fetch recent audit logs. HTTP: " + response.statusCode());
      }
    } catch (Exception e) {
      throw new SQLException("Failed to fetch recent logs from REST API", e);
    }
  }
}
