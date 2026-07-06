package com.chequeprint.service;

import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.util.PasswordUtil;
import com.chequeprint.util.SessionManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;

public class UserService {

    private static final String BASE_URL = "http://localhost:8081/api/users";
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper;

    public UserService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpRequest.Builder requestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
        String token = SessionManager.getJwtToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    // ── Profile CRUD ─────────────────────────────────────────────────
    
    public User loadProfile() throws SQLException {
        try {
            int userId = SessionManager.requireUser().getId();
            HttpRequest request = requestBuilder(BASE_URL + "/" + userId)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), User.class);
            } else {
                throw new IOException("Failed to load profile. HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("Failed to load profile via REST API", e);
        }
    }

    public boolean saveProfile(User user) throws SQLException {
        validateUser(user);
        try {
            String json = objectMapper.writeValueAsString(user);
            HttpRequest request = requestBuilder(BASE_URL + "/" + user.getId())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            throw new SQLException("Failed to update profile via REST API", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private void validateUser(User user) {
        if (user == null)
            throw new IllegalArgumentException("User must not be null.");
        if (user.getName() == null || user.getName().isBlank())
            throw new IllegalArgumentException("Name is required.");
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && !user.getEmail().contains("@"))
            throw new IllegalArgumentException("Invalid email address.");
    }

    public static String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    public void changePassword(int userId, String currentPassword, String newPassword) throws SQLException {
        try {
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("currentPassword", currentPassword);
            payload.put("newPassword", newPassword);

            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = requestBuilder(BASE_URL + "/" + userId + "/change-password")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String error = response.body();
                if (error == null || error.isBlank()) {
                    error = "Failed to update password. HTTP: " + response.statusCode();
                }
                throw new IllegalArgumentException(error);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Failed to connect to password change API", e);
        }
    }

    // ── Admin user management helpers ─────────────────────────────────
    public List<User> findAllUsers(User actor) throws SQLException {
        try {
            HttpRequest request = requestBuilder(BASE_URL)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            } else {
                throw new IOException("Failed to load users list. HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("Failed to load users list via REST API", e);
        }
    }

    public boolean userExists(String usernameOrEmail) throws SQLException {
        try {
            HttpRequest request = requestBuilder(BASE_URL + "/username/" + usernameOrEmail)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void registerUser(String username, String password, UserRole role, User createdBy) throws SQLException {
        try {
            User u = new User();
            u.setUsername(username);
            u.setName(username);
            u.setEmail(username.contains("@") ? username : username + "@example.com");
            u.setPassword(password);
            u.setRole((createdBy != null && AccessControl.can(createdBy, Permission.MANAGE_USERS) && role != null)
                    ? role.label()
                    : UserRole.OPERATOR.label());
            u.setStatus("Active");
            validateUser(u);

            String json = objectMapper.writeValueAsString(u);
            HttpRequest request = requestBuilder(BASE_URL)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new IOException("Failed to register user. HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("Registration failed: " + e.getMessage(), e);
        }
    }

    public void createUser(User actor, String username, String name, String email, String password, UserRole role)
            throws SQLException {
        try {
            User u = new User();
            u.setUsername(username);
            u.setName(name == null || name.isBlank() ? username : name);
            u.setEmail(email);
            u.setPassword(password);
            u.setRole(role == null ? UserRole.OPERATOR.label() : role.label());
            u.setStatus("Active");
            validateUser(u);

            String json = objectMapper.writeValueAsString(u);
            HttpRequest request = requestBuilder(BASE_URL)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new IOException("Failed to create user. HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("User creation failed: " + e.getMessage(), e);
        }
    }

    public void updateUser(User actor, User target, String newPassword) throws SQLException {
        if (newPassword != null && !newPassword.isBlank()) {
            target.setPassword(newPassword);
        } else {
            target.setPassword(null);
        }
        if (!saveProfile(target)) {
            throw new SQLException("Failed to update user profile.");
        }
    }

    public void deleteUser(User actor, int userId) throws SQLException {
        try {
            HttpRequest request = requestBuilder(BASE_URL + "/" + userId)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204) {
                throw new IOException("Failed to delete user. HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("User deletion failed: " + e.getMessage(), e);
        }
    }

    public List<com.chequeprint.model.AuditLog> loadUserActivity(int userId) throws SQLException {
        try {
            HttpRequest request = requestBuilder(BASE_URL + "/" + userId + "/activity")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, com.chequeprint.model.AuditLog.class));
            } else {
                throw new IOException("Failed to load activity logs. HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("Failed to load user activity via REST API", e);
        }
    }
}