package com.chequeprint.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Reusable API Client for JavaFX that manages HTTP request operations,
 * Authorization token injection, JSON ↔ Java object conversion via Jackson,
 * and 401 Unauthorized redirect triggers.
 */
public class ApiClient {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ==========================================
    // HIGH-LEVEL OBJECT MAPPING METHODS (REUSABLE)
    // ==========================================

    /**
     * Sends a GET request and deserializes the JSON response into a Java object.
     */
    public static <T> T getObject(String urlStr, Class<T> responseType) throws Exception {
        String json = get(urlStr);
        return objectMapper.readValue(json, responseType);
    }

    /**
     * Sends a GET request and deserializes the JSON response into a List of Java objects.
     */
    public static <T> List<T> getList(String urlStr, Class<T> elementType) throws Exception {
        String json = get(urlStr);
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    /**
     * Sends a POST request containing a serialized JSON body, and deserializes the response.
     */
    public static <T> T postObject(String urlStr, Object requestBody, Class<T> responseType) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        String responseJson = post(urlStr, jsonBody);
        if (responseType == null || responseJson == null || responseJson.isBlank()) {
            return null;
        }
        return objectMapper.readValue(responseJson, responseType);
    }

    // ==========================================
    // CORE HTTP CONNECTION METHODS
    // ==========================================

    public static String get(String urlStr) throws Exception {
        return execute(urlStr, "GET", null);
    }

    public static String post(String urlStr, String jsonBody) throws Exception {
        return execute(urlStr, "POST", jsonBody);
    }

    public static String put(String urlStr, String jsonBody) throws Exception {
        return execute(urlStr, "PUT", jsonBody);
    }

    public static String delete(String urlStr) throws Exception {
        return execute(urlStr, "DELETE", null);
    }

    private static String execute(String urlStr, String method, String jsonBody) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Add Authorization header
            String token = SessionManager.getInstance().getToken();
            if (token != null && !token.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();

            // Handle 401 Unauthorized
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                SessionManager.getInstance().clear();
                
                // Redirect on FX thread
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
                            FXMLLoader loader = new FXMLLoader(ApiClient.class.getResource("/view/login.fxml"));
                            Parent root = loader.load();
                            Scene scene = new Scene(root, 900, 620);
                            var stylesheet = ApiClient.class.getResource("/css/style.css");
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

            if (responseCode >= 200 && responseCode < 300) {
                InputStream is = connection.getInputStream();
                return readStream(is);
            } else {
                InputStream es = connection.getErrorStream();
                String errorMsg = es != null ? readStream(es) : "";
                throw new RuntimeException("HTTP Request Failed: " + responseCode + " - " + errorMsg);
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream is) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString().trim();
    }
}
