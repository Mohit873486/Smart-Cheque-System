package com.chequeprint.service;

import com.chequeprint.model.BankAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.chequeprint.util.SessionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ApiService {

    private static final String API_URL = "http://localhost:8081/api/bank/account";
    private final ObjectMapper mapper;

    public ApiService() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<BankAccount> getBankAccounts() throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            
            String authHeader = com.chequeprint.util.Session.getAuthorizationHeader();
            if (!authHeader.isBlank()) {
                connection.setRequestProperty("Authorization", authHeader);
            }
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                // Parse JSON response
                return mapper.readValue(response.toString(), new TypeReference<List<BankAccount>>() {});
            } else if (status == 409 || status == 404) {
                // Return empty list gracefully instead of throwing exception for no data
                return java.util.Collections.emptyList();
            } else {
                throw new Exception("Failed to load data. HTTP Status: " + status);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public BankAccount saveBankAccount(BankAccount account) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://localhost:8081/api/bank/account");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            String authHeader = com.chequeprint.util.Session.getAuthorizationHeader();
            if (!authHeader.isBlank()) {
                connection.setRequestProperty("Authorization", authHeader);
            }
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            String jsonPayload = mapper.writeValueAsString(account);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                return mapper.readValue(response.toString(), BankAccount.class);
            } else {
                throw new Exception("Failed to save data. HTTP Status: " + status);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
