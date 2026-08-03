package com.chequeprint.api;

import com.chequeprint.util.RestApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiGateway {
    private final ObjectMapper mapper;

    public ApiGateway() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> ApiResponse<T> get(String url, TypeReference<T> type) throws Exception {
        HttpRequest request = RestApiClient.requestBuilder(url).GET().build();
        return send(request, type);
    }

    public <T> ApiResponse<T> get(String url, Class<T> type) throws Exception {
        HttpRequest request = RestApiClient.requestBuilder(url).GET().build();
        return send(request, type);
    }

    public <T> ApiResponse<T> post(String url, Object payload, Class<T> type) throws Exception {
        HttpRequest request = RestApiClient.requestBuilder(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
        return send(request, type);
    }

    public <T> ApiResponse<T> put(String url, Object payload, Class<T> type) throws Exception {
        HttpRequest request = RestApiClient.requestBuilder(url)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
        return send(request, type);
    }

    public ApiResponse<Void> putNoBody(String url) throws Exception {
        HttpRequest request = RestApiClient.requestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendVoid(request);
    }

    public ApiResponse<Void> delete(String url) throws Exception {
        HttpRequest request = RestApiClient.requestBuilder(url).DELETE().build();
        return sendVoid(request);
    }

    public <T> ApiResponse<T> send(HttpRequest request, Class<T> type) throws Exception {
        HttpResponse<String> response = RestApiClient.send(request);
        if (!isSuccessful(response.statusCode())) {
            return ApiResponse.failure(response.statusCode(), response.body(), response.body());
        }
        if (type == Void.class || response.body() == null || response.body().isBlank()) {
            return ApiResponse.success(response.statusCode(), null, response.body());
        }
        return ApiResponse.success(response.statusCode(), mapper.readValue(response.body(), type), response.body());
    }

    public <T> ApiResponse<T> send(HttpRequest request, TypeReference<T> type) throws Exception {
        HttpResponse<String> response = RestApiClient.send(request);
        if (!isSuccessful(response.statusCode())) {
            return ApiResponse.failure(response.statusCode(), response.body(), response.body());
        }
        if (response.body() == null || response.body().isBlank()) {
            return ApiResponse.success(response.statusCode(), null, response.body());
        }
        return ApiResponse.success(response.statusCode(), mapper.readValue(response.body(), type), response.body());
    }

    public ApiResponse<Void> sendVoid(HttpRequest request) throws Exception {
        HttpResponse<String> response = RestApiClient.send(request);
        if (!isSuccessful(response.statusCode())) {
            return ApiResponse.failure(response.statusCode(), response.body(), response.body());
        }
        return ApiResponse.success(response.statusCode(), null, response.body());
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
