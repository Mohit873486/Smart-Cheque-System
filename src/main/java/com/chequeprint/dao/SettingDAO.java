package com.chequeprint.dao;

import com.chequeprint.model.Settings;
import com.chequeprint.util.RestApiClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;

public class SettingDAO {

    private final ObjectMapper objectMapper;

    public SettingDAO() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Settings getSettings() throws SQLException {
        try {
            HttpRequest request = RestApiClient.requestBuilder("http://localhost:8081/api/settings")
                    .GET()
                    .build();
            HttpResponse<String> response = RestApiClient.send(request);
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Settings.class);
            } else {
                throw new IOException("Failed to load settings. HTTP status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("Error loading settings from REST API", e);
        }
    }

    public void saveSettings(Settings s) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(s);
            HttpRequest request = RestApiClient.requestBuilder("http://localhost:8081/api/settings")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = RestApiClient.send(request);
            if (response.statusCode() != 200) {
                throw new IOException("Failed to save settings. HTTP status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SQLException("Error saving settings via REST API", e);
        }
    }
}
