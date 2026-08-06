package com.chequeprint.dao;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.User;
import com.chequeprint.util.Session;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO {

    private static final String API_USERS = ApiConfig.BASE_URL + "/api/users";
    private static volatile long lastErrorLogTime = 0;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UserDAO() {
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

    private void logError(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime > 5000) {
            lastErrorLogTime = now;
            System.err.println(message);
        }
    }

    public List<User> findAll() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_USERS))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), new TypeReference<List<User>>() {});
            }
        } catch (Exception ex) {
            logError("UserDAO findAll error: " + ex.getMessage());
        }
        return new ArrayList<>();
    }

    public User findById(int id) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_USERS + "/" + id))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), User.class);
            }
        } catch (Exception ex) {
            logError("UserDAO findById error: " + ex.getMessage());
        }
        return null;
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_USERS + "/search?query=" + usernameOrEmail))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                List<User> users = objectMapper.readValue(response.body(), new TypeReference<List<User>>() {});
                return users.isEmpty() ? null : users.get(0);
            }
        } catch (Exception ex) {
            logError("UserDAO findByUsernameOrEmail error: " + ex.getMessage());
        }
        return null;
    }

    public boolean insert(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_USERS))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO insert error: " + ex.getMessage());
            return false;
        }
    }

    public boolean update(User user) {
        if (user == null || user.getId() <= 0) return false;
        try {
            String json = objectMapper.writeValueAsString(user);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_USERS + "/" + user.getId()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO update error: " + ex.getMessage());
            return false;
        }
    }

    public boolean deleteById(int id) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(API_USERS + "/" + id));
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO delete error: " + ex.getMessage());
            return false;
        }
    }

    // ========== ForgotPasswordService compatibility methods ==========

    public boolean createPasswordResetOtp(int userId, String otpHash, java.time.LocalDateTime expiresAt) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("otpHash", otpHash);
            body.put("expiresAt", expiresAt.toString());
            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_USERS + "/otp"))
                    .header("Content-Type", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO createPasswordResetOtp error: " + ex.getMessage());
            return false;
        }
    }

    public String findActiveOtpHash(int userId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_USERS + "/" + userId + "/otp"))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception ex) {
            logError("UserDAO findActiveOtpHash error: " + ex.getMessage());
        }
        return null;
    }

    public boolean updatePassword(int userId, String passwordHash) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("password", passwordHash);
            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_USERS + "/" + userId + "/password"))
                    .header("Content-Type", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO updatePassword error: " + ex.getMessage());
            return false;
        }
    }

    public boolean resetLoginAttempts(int userId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .uri(URI.create(API_USERS + "/" + userId + "/unlock"))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO resetLoginAttempts error: " + ex.getMessage());
            return false;
        }
    }

    public boolean markOtpsUsed(int userId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .uri(URI.create(API_USERS + "/" + userId + "/otp/used"))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("UserDAO markOtpsUsed error: " + ex.getMessage());
            return false;
        }
    }

    public boolean insertOrUpdate(User u) {
        if (u.getId() == 0) {
            return insert(u);
        } else {
            return update(u);
        }
    }
}