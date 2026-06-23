package com.chequeprint.util;

import com.chequeprint.model.Cheque;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

public class ChequeApiClient {

    private static final String BASE_URL = "http://localhost:8080/api/cheques";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChequeApiClient() {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
        
        // Add custom LocalDate serializer/deserializer to avoid dependency on jackson-datatype-jsr310 module
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeString(value.toString());
                } else {
                    gen.writeNull();
                }
            }
        });
        module.addDeserializer(LocalDate.class, new JsonDeserializer<>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String val = p.getValueAsString();
                return (val == null || val.isBlank()) ? null : LocalDate.parse(val);
            }
        });
        this.objectMapper.registerModule(module);
    }

    public List<Cheque> getAllCheques() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Cheque.class));
        } else {
            throw new IOException("Failed to fetch cheques. HTTP: " + response.statusCode());
        }
    }

    public Cheque getChequeById(int id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Cheque.class);
        } else if (response.statusCode() == 404) {
            return null;
        } else {
            throw new IOException("Failed to get cheque by ID. HTTP: " + response.statusCode());
        }
    }

    public Cheque createCheque(Cheque cheque) throws Exception {
        String json = objectMapper.writeValueAsString(cheque);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), Cheque.class);
        } else {
            throw new IOException("Failed to create cheque. HTTP: " + response.statusCode());
        }
    }

    public boolean updateCheque(Cheque cheque) throws Exception {
        String json = objectMapper.writeValueAsString(cheque);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + cheque.getId()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 200;
    }

    public boolean deleteCheque(int id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;
    }

    public boolean existsByChequeNo(String chequeNo, int excludeId) throws Exception {
        String url = BASE_URL + "/exists?chequeNo=" + chequeNo + "&excludeId=" + excludeId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return Boolean.parseBoolean(response.body());
        } else {
            throw new IOException("Failed to check cheque number existence. HTTP: " + response.statusCode());
        }
    }
}
