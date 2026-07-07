package com.chequeprint.service;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.BankAccount;
import com.chequeprint.util.AccessDeniedException;
import com.chequeprint.util.SessionManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Service class for Bank Account API operations, utilizing java.net.http.HttpClient.
 */
public class BankAccountService {

    private static final String API_URL = ApiConfig.BASE_URL + "/api/bank/account";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BankAccountService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Fetches all bank accounts from the backend using HttpClient.
     * Injects the Authorization Bearer Token. Handles 401 and parsing failures.
     */
    public List<BankAccount> fetchAccounts() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(10))
                .GET();

        // Inject JWT bearer authorization token
        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        // Handle 401 session expiration
        if (statusCode == 401) {
            SessionManager.getInstance().clear();
            
            // Redirect back to login screen on JavaFX Thread
            Platform.runLater(() -> {
                try {
                    Stage stage = null;
                    for (Window window : Window.getWindows()) {
                        if (window instanceof Stage s && s.isShowing()) {
                            stage = s;
                            break;
                        }
                    }
                    if (stage != null) {
                        stage.setMaximized(false);
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root, 900, 620);
                        var stylesheet = getClass().getResource("/css/style.css");
                        if (stylesheet != null) {
                            scene.getStylesheets().add(stylesheet.toExternalForm());
                        }
                        stage.setScene(scene);
                        stage.setTitle("Smart Cheque Management System - Sign In");
                        stage.centerOnScreen();

                        Alert alert = new Alert(Alert.AlertType.WARNING, "Session expired");
                        alert.setTitle("Session Expired");
                        alert.setHeaderText(null);
                        alert.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            throw new AccessDeniedException("Unauthorized: 401 returned from server.");
        }

        if (statusCode >= 200 && statusCode < 300) {
            String body = response.body();
            if (body == null || body.isBlank() || "[]".equals(body.trim())) {
                return List.of(); // Return empty list on empty response
            }
            return objectMapper.readValue(body,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BankAccount.class));
        } else {
            throw new IOException("HTTP Request Failed with Status Code: " + statusCode + " - " + response.body());
        }
    }

    /**
     * Saves a bank account to the backend database.
     * Maps the object to JSON and sends a POST request.
     */
    public BankAccount saveAccount(BankAccount account) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(account);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        // Inject JWT bearer authorization token
        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        // 401 Unauthorized handling
        if (statusCode == 401) {
            SessionManager.getInstance().clear();
            
            Platform.runLater(() -> {
                try {
                    Stage stage = null;
                    for (Window window : Window.getWindows()) {
                        if (window instanceof Stage s && s.isShowing()) {
                            stage = s;
                            break;
                        }
                    }
                    if (stage != null) {
                        stage.setMaximized(false);
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root, 900, 620);
                        var stylesheet = getClass().getResource("/css/style.css");
                        if (stylesheet != null) {
                            scene.getStylesheets().add(stylesheet.toExternalForm());
                        }
                        stage.setScene(scene);
                        stage.setTitle("Smart Cheque Management System - Sign In");
                        stage.centerOnScreen();

                        Alert alert = new Alert(Alert.AlertType.WARNING, "Session expired");
                        alert.setTitle("Session Expired");
                        alert.setHeaderText(null);
                        alert.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            throw new AccessDeniedException("Session expired.");
        }

        if (statusCode == 200 || statusCode == 201) {
            return objectMapper.readValue(response.body(), BankAccount.class);
        } else {
            throw new IOException("HTTP Request Failed with Status Code: " + statusCode + " - " + response.body());
        }
    }
}
