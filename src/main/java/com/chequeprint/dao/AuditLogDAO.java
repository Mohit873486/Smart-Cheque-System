  package com.chequeprint.dao;

  import com.chequeprint.config.ApiConfig;
  import com.chequeprint.model.AuditLog;
  import com.chequeprint.util.Session;
  import com.fasterxml.jackson.databind.DeserializationFeature;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import com.fasterxml.jackson.core.type.TypeReference;

  import java.net.URI;
  import java.net.http.HttpClient;
  import java.net.http.HttpRequest;
  import java.net.http.HttpResponse;
  import java.util.ArrayList;
  import java.util.List;
  import java.time.Duration;

  public class AuditLogDAO {

      private static final String API_AUDIT = ApiConfig.BASE_URL + "/api/audit-logs";
      private static volatile long lastErrorLogTime = 0;

      private final HttpClient httpClient;
      private final ObjectMapper objectMapper;

      public AuditLogDAO() {
          this.httpClient = HttpClient.newBuilder()
                  .version(HttpClient.Version.HTTP_2)
                  .connectTimeout(Duration.ofSeconds(10))
                  .build();
          this.objectMapper = new ObjectMapper();
          this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      }

      private void addAuthToken(HttpRequest.Builder builder) {
          String authHeader = Session.getAuthorizationHeader();
          if (authHeader != null && !authHeader.isBlank()) {
              builder.header("Authorization", authHeader);
          }
      }

      public boolean insert(AuditLog log) {
          try {
              String json = objectMapper.writeValueAsString(log);
              HttpRequest.Builder builder = HttpRequest.newBuilder()
                      .POST(HttpRequest.BodyPublishers.ofString(json))
                      .uri(URI.create(API_AUDIT))
                      .header("Content-Type", "application/json")
                      .header("Accept", "application/json");
              addAuthToken(builder);
              HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
              return response.statusCode() >= 200 && response.statusCode() < 300;
          } catch (Exception ex) {
              System.err.println("AuditLogDAO insert error: " + ex.getMessage());
              return false;
          }
      }

      public List<AuditLog> findRecent(int limit) {
          try {
              HttpRequest.Builder builder = HttpRequest.newBuilder()
                      .GET()
                      .uri(URI.create(API_AUDIT + "?limit=" + limit))
                      .header("Accept", "application/json");
              addAuthToken(builder);
              HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
              if (response.statusCode() >= 200 && response.statusCode() < 300) {
                  return objectMapper.readValue(response.body(), new TypeReference<List<AuditLog>>() {});
              }
          } catch (Exception ex) {
              System.err.println("AuditLogDAO findRecent error: " + ex.getMessage());
          }
          return new ArrayList<>();
      }
  }