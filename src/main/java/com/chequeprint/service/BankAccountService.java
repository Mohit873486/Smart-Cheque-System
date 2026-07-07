package com.chequeprint.service;

import com.chequeprint.util.AccessDeniedException;
import com.chequeprint.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BankAccountService {

    private static final String DEFAULT_URL = "http://localhost:8081/api/bank/account";

    /**
     * Calls the default bank account details API endpoint.
     *
     * @return response content as a String
     * @throws Exception if connection fails or response code is not 200
     */
    public String getBankAccountDetails() throws Exception {
        return getBankAccountDetails(DEFAULT_URL);
    }

    /**
     * Calls the specified bank account details API endpoint.
     *
     * @param urlStr the URL of the bank account API endpoint to call
     * @return response content as a String
     * @throws Exception if connection fails or response code is not 200
     */
    public String getBankAccountDetails(String urlStr) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Add Authorization header: "Bearer " + SessionManager.getToken()
            String token = SessionManager.getToken();
            System.out.println("Debug - Token Value: " + token);
            if (token != null && !token.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Debug - API Response Code: " + responseCode);

            // Handle 401 Unauthorized error
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                System.err.println("Debug: 401 Unauthorized returned from " + urlStr);
                SessionManager.clear();

                // Redirect to login page and show "Session expired" alert
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
                            stage.setMaximized(false); // Restore normal window size
                            
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

                throw new AccessDeniedException("Unauthorized: 401 error code returned from server. Session cleared.");
            }

            // If response is successful (200 OK)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String body = readStream(connection.getInputStream());
                System.out.println("Debug - API Response Body: " + body);
                return body;
            } else {
                // Read error stream if available, otherwise throw exception
                String errorMsg = "";
                try {
                    InputStream errStream = connection.getErrorStream();
                    if (errStream != null) {
                        errorMsg = readStream(errStream);
                    }
                } catch (Exception ignored) {}
                System.out.println("Debug - API Response Body (Error): " + errorMsg);
                throw new IOException("HTTP Request Failed with code: " + responseCode + ". Error: " + errorMsg);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream is) throws IOException {
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
