package com.chequeprint.service;

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
  private int remainingLoginAttempts = MAX_LOGIN_ATTEMPTS;
  private User currentUser;
  private boolean accountLocked = false;

  public AuthenticationResult authenticate(String usernameOrEmail, String password) {
    if (usernameOrEmail == null || usernameOrEmail.isBlank()
        || password == null || password.isBlank()) {
      return AuthenticationResult.failure("Username/email and password are required.");
    }

    if (accountLocked) {
      return AuthenticationResult.failure("Blocked account. Contact an administrator to unlock it.");
    }

    try {
      // 1. Prepare request body JSON
      Map<String, String> requestBody = new HashMap<>();
      requestBody.put("username", usernameOrEmail.trim());
      requestBody.put("password", password);
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);

      // 2. Build HTTP POST request
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:8081/api/auth/login"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
              .build();

      // 3. Send request
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        // Successful login
        JsonNode rootNode = objectMapper.readTree(response.body());
        String token = rootNode.get("token").asText();
        JsonNode userNode = rootNode.get("user");

        // 4. Map properties from userNode to client User model
        User user = new User();
        user.setId(userNode.get("id").asInt());
        user.setUsername(userNode.get("username").asText());
        user.setName(userNode.get("name").asText());
        user.setEmail(userNode.get("email").asText());
        user.setRole(userNode.get("role").asText());
        user.setStatus("Active");

        // 5. Store session context
        SessionManager.start(user);
        SessionManager.setJwtToken(token);
        
        remainingLoginAttempts = MAX_LOGIN_ATTEMPTS;
        currentUser = user;
        
        auditService.recordLogin(user);
        return AuthenticationResult.success(user);

      } else if (response.statusCode() == 401) {
        // Bad credentials
        remainingLoginAttempts--;
        if (remainingLoginAttempts <= 0) {
          accountLocked = true;
          return AuthenticationResult.failure("Blocked account. Maximum login attempts reached.");
        }
        return AuthenticationResult.failure("Wrong password. " + remainingLoginAttempts + " attempt(s) remaining.");
      } else if (response.statusCode() == 403) {
        // Locked / Disabled user
        return AuthenticationResult.failure("Blocked account. Contact an administrator to unlock it.");
      } else {
        // General error details
        JsonNode errNode = objectMapper.readTree(response.body());
        String msg = errNode.has("message") ? errNode.get("message").asText() : "HTTP error: " + response.statusCode();
        return AuthenticationResult.failure("Login failed: " + msg);
      }

    } catch (Exception e) {
      return AuthenticationResult.failure("REST server unavailable: " + e.getMessage());
    }
  }

  public AuthenticationResult authenticate(String usernameOrEmail, String password, String ignoredRoleName) {
    return authenticate(usernameOrEmail, password);
  }

  public boolean isLocked() {
    return accountLocked || remainingLoginAttempts <= 0;
  }

  public int getRemainingLoginAttempts() {
    return remainingLoginAttempts;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void logout() {
    currentUser = null;
    remainingLoginAttempts = MAX_LOGIN_ATTEMPTS;
    accountLocked = false;
    SessionManager.clear();
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
