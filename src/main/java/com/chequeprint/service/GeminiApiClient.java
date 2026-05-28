package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeminiApiClient {

  private static final String GEMINI_API_KEY_ENV = "GEMINI_API_KEY";
  private static final String API_ENDPOINT_FORMAT = "https://generativelanguage.googleapis.com/v1beta2/models/{model}:generateContent";

  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public GeminiApiClient() {
    this(HttpClient.newHttpClient());
  }

  GeminiApiClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public String generateText(String model, List<Map<String, Object>> messages, Map<String, Object> extraPayload)
      throws Exception {
    String apiKey = System.getenv(GEMINI_API_KEY_ENV);
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(GEMINI_API_KEY_ENV + " environment variable is not set.");
    }

    Map<String, Object> prompt = Map.of("messages", messages);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", model);
    payload.put("prompt", prompt);

    if (extraPayload != null) {
      payload.putAll(extraPayload);
    }

    String url = String.format(API_ENDPOINT_FORMAT, URLEncoder.encode(model, StandardCharsets.UTF_8));
    boolean useBearerHeader = apiKey.trim().toLowerCase().startsWith("bearer ");
    if (!useBearerHeader) {
      url = url + "?key=" + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8);
    }

    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));

    if (useBearerHeader) {
      requestBuilder.header("Authorization", apiKey.trim());
    }

    HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Gemini request failed: HTTP "
          + response.statusCode() + " - " + response.body());
    }

    return extractText(response.body());
  }

  private String extractText(String responseBody) throws Exception {
    JsonNode root = mapper.readTree(responseBody);
    StringBuilder text = new StringBuilder();

    JsonNode candidates = root.path("candidates");
    if (candidates.isArray()) {
      for (JsonNode candidate : candidates) {
        if (candidate.hasNonNull("output")) {
          text.append(candidate.path("output").asText());
        }
        JsonNode content = candidate.path("content");
        if (content.isArray()) {
          for (JsonNode part : content) {
            if (part.hasNonNull("text")) {
              text.append(part.path("text").asText());
            }
          }
        }
      }
    }

    JsonNode output = root.path("output");
    if (output.isTextual()) {
      text.append(output.asText());
    } else if (output.isArray()) {
      for (JsonNode item : output) {
        if (item.hasNonNull("text")) {
          text.append(item.path("text").asText());
        }
      }
    }

    if (text.toString().isBlank()) {
      JsonNode firstCandidate = root.path("candidates").path(0);
      if (firstCandidate.hasNonNull("output")) {
        text.append(firstCandidate.path("output").asText());
      } else {
        JsonNode fallbackContent = firstCandidate.path("content");
        if (fallbackContent.isArray()) {
          for (JsonNode part : fallbackContent) {
            if (part.hasNonNull("text")) {
              text.append(part.path("text").asText());
            }
          }
        }
      }
    }

    return text.toString().trim();
  }
}
