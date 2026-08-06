package com.chequeprint.service;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.User;
import com.chequeprint.util.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AuditService auditService = new AuditService();
  private static final int MAX_LOGIN_ATTEMPTS = 3;
  private User currentUser;

  public AuthenticationResult authenticate(String usernameOrEmail, String password) {
    if (usernameOrEmail == null || usernameOrEmail.isBlank()
        || password == null || password.isBlank()) {
      return AuthenticationResult.failure("Username/email and password are required.");
    }

    try {
      Map<String, String> requestBody = new HashMap<>();
      requestBody.put("username", usernameOrEmail.trim());
      requestBody.put("password", password);
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);

      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(ApiConfig.BASE_URL + "/api/auth/login"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
              .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        JsonNode rootNode = objectMapper.readTree(response.body());
        String token = rootNode.get("token").asText();
        JsonNode userNode = rootNode.get("user");

        User user = new User();
        user.setId(userNode.get("id").asInt());
        user.setUsername(userNode.get("username").asText());
        user.setName(userNode.get("name").asText());
        user.setEmail(userNode.get("email").asText());
        user.setRole(userNode.get("role").asText());
        user.setStatus("Active");

        com.chequeprint.util.Session.token = token;
        com.chequeprint.util.Session.username = user.getUsername();
        com.chequeprint.util.Session.setSession(token, user.getUsername());
        SessionManager.getInstance().start(user);
        SessionManager.getInstance().setToken(token);
        
        currentUser = user;
        auditService.recordLogin(user);
        return AuthenticationResult.success(user);

      } else if (response.statusCode() == 401) {
        JsonNode errNode = objectMapper.readTree(response.body());
        boolean locked = errNode.has("locked") && errNode.get("locked").asBoolean();
        if (locked) {
          return AuthenticationResult.failure("Blocked account. Contact an administrator to unlock it.");
        }
        int remaining = errNode.has("remainingAttempts") ? errNode.get("remainingAttempts").asInt() : 0;
        return AuthenticationResult.failure("Wrong password. " + remaining + " attempt(s) remaining.");

      } else if (response.statusCode() == 403) {
        JsonNode errNode = objectMapper.readTree(response.body());
        String msg = errNode.has("message") ? errNode.get("message").asText() : "Account is locked.";
        return AuthenticationResult.failure(msg);

      } else {
        JsonNode errNode = objectMapper.readTree(response.body());
        String msg = errNode.has("message") ? errNode.get("message").asText() : "HTTP error: " + response.statusCode();
        return AuthenticationResult.failure("Login failed: " + msg);
      }

    } catch (Exception e) {
      String msg = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
      return AuthenticationResult.failure("REST server unavailable (" + msg + "). Check if backend is running on " + ApiConfig.BASE_URL + ".");
    }
  }

  public AuthenticationResult authenticate(String usernameOrEmail, String password, String ignoredRoleName) {
    return authenticate(usernameOrEmail, password);
  }

  public boolean isLocked() {
    return false;
  }

  public int getRemainingLoginAttempts() {
    return MAX_LOGIN_ATTEMPTS;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void logout() {
    currentUser = null;
    SessionManager.getInstance().clear();
  }

  public String getLandingPage() {
    if (currentUser == null) {
      return "dashboard";
    }
    String[] preferredPages = {"dashboard", "cheques", "invoices", "banks", "profile", "support"};
    for (String page : preferredPages) {
      if (AccessControl.canAccessPage(currentUser, page)) {
        return page;
      }
    }
    return "support";
  }

  public boolean canAccessPage(String page) {
    return AccessControl.canAccessPage(currentUser, page);
  }
}