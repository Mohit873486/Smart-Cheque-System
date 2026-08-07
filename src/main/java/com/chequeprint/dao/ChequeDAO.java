package com.chequeprint.dao;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.ApiResponse;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.PageRequest;
import com.chequeprint.model.PageResult;
import com.chequeprint.util.RestApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Paginated, resilient ChequeDAO.
 *
 * <p>
 * <b>Backend Contract (ideal):</b>
 * </p>
 * <ul>
 * <li>GET /api/cheques?page=0&size=25&sort=issueDate,desc&q=term</li>
 * <li>GET /api/cheques/{id}</li>
 * <li>POST /api/cheques</li>
 * <li>PUT /api/cheques/{id}</li>
 * <li>DELETE /api/cheques/{id}</li>
 * <li>GET /api/cheques/stats/summary</li>
 * <li>GET /api/cheques/exists?chequeNo=XXX&excludeId=YYY</li>
 * </ul>
 *
 * <p>
 * <b>Backward Compatibility:</b> Agar backend plain array {@code [{...},{...}]}
 * bhi bheje,
 * toh DAO usse {@code PageResult} mein wrap kar dega. Pagination tab activate
 * hoga jab
 * backend Spring Data {@code Page<T>} return karega.
 * </p>
 */
public class ChequeDAO {

    private static final String API_CHEQUES = ApiConfig.BASE_URL + "/api/cheques";
    private static final int MAX_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 500;
    private static final AtomicLong LAST_ERROR_LOG_TIME = new AtomicLong(0);
    private static final long ERROR_LOG_COOLDOWN_MS = 5000;

    private final ObjectMapper objectMapper;

    public ChequeDAO() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // LocalDate support (same as old DAO)
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new com.fasterxml.jackson.databind.JsonSerializer<>() {
            @Override
            public void serialize(LocalDate value, com.fasterxml.jackson.core.JsonGenerator gen,
                    com.fasterxml.jackson.databind.SerializerProvider serializers) throws IOException {
                if (value != null)
                    gen.writeString(value.toString());
                else
                    gen.writeNull();
            }
        });
        module.addDeserializer(LocalDate.class, new com.fasterxml.jackson.databind.JsonDeserializer<>() {
            @Override
            public LocalDate deserialize(com.fasterxml.jackson.core.JsonParser p,
                    com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
                String val = p.getValueAsString();
                return (val == null || val.isBlank()) ? null : LocalDate.parse(val);
            }
        });
        this.objectMapper.registerModule(module);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. PAGINATED READS
    // ═══════════════════════════════════════════════════════════════════════

    public PageResult<Cheque> findAll(PageRequest pageRequest) {
        String url = API_CHEQUES + "?" + pageRequest.toQueryString();
        ApiResponse<PageResult<Cheque>> resp = executeGetPage(url);
        return resp.isOk() && resp.getData() != null ? resp.getData() : emptyPage(pageRequest);
    }

    public PageResult<Cheque> search(String query, PageRequest pageRequest) {
        if (query == null || query.isBlank()) {
            return findAll(pageRequest);
        }
        PageRequest searchReq = PageRequest.of(pageRequest.getPage(), pageRequest.getSize())
                .withSort(pageRequest.getSortBy(), pageRequest.getSortDir())
                .withSearch(query);
        String url = API_CHEQUES + "?" + searchReq.toQueryString();
        ApiResponse<PageResult<Cheque>> resp = executeGetPage(url);
        return resp.isOk() && resp.getData() != null ? resp.getData() : emptyPage(pageRequest);
    }

    public Optional<Cheque> findById(int id) {
        ApiResponse<Cheque> resp = executeGet(API_CHEQUES + "/" + id, new TypeReference<>() {
        });
        return resp.isOk() ? Optional.ofNullable(resp.getData()) : Optional.empty();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. WRITE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    public ApiResponse<Cheque> insert(Cheque cheque) {
        try {
            String json = objectMapper.writeValueAsString(cheque);
            HttpRequest request = RestApiClient.requestBuilder(API_CHEQUES)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return executeWithRetry(request, new TypeReference<>() {
            });
        } catch (Exception e) {
            return logAndWrapError("insert", e);
        }
    }

    public ApiResponse<Void> update(Cheque cheque) {
        if (cheque == null || cheque.getId() <= 0) {
            return ApiResponse.error("Invalid cheque or ID", 400);
        }
        try {
            String json = objectMapper.writeValueAsString(cheque);
            HttpRequest request = RestApiClient.requestBuilder(API_CHEQUES + "/" + cheque.getId())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return executeWithRetry(request, new TypeReference<>() {
            });
        } catch (Exception e) {
            return logAndWrapError("update", e);
        }
    }

    public ApiResponse<Void> delete(int id) {
        HttpRequest request = RestApiClient.requestBuilder(API_CHEQUES + "/" + id)
                .DELETE()
                .build();
        return executeWithRetry(request, new TypeReference<>() {
        });
    }

    public ApiResponse<Void> updateStatus(int chequeId, Cheque.Status status) {
        try {
            String json = objectMapper.writeValueAsString(Collections.singletonMap("status", status.name()));
            HttpRequest request = RestApiClient.requestBuilder(API_CHEQUES + "/" + chequeId + "/status")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return executeWithRetry(request, new TypeReference<>() {
            });
        } catch (Exception e) {
            return logAndWrapError("updateStatus", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. SERVER-SIDE STATS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Expected: { "total": 100, "pending": 20, "printed": 50, "today": 5,
     * "monthlySum": 150000.0 }
     */
    public ApiResponse<ChequeStats> fetchStats() {
        return executeGet(API_CHEQUES + "/stats/summary", new TypeReference<>() {
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. VALIDATION / UTILITY
    // ═══════════════════════════════════════════════════════════════════════

    public boolean existsByChequeNo(String chequeNo, Integer excludeId) {
        if (chequeNo == null || chequeNo.isBlank())
            return false;
        StringBuilder url = new StringBuilder(API_CHEQUES + "/exists?chequeNo=" + encode(chequeNo));
        if (excludeId != null)
            url.append("&excludeId=").append(excludeId);
        ApiResponse<Boolean> resp = executeGet(url.toString(), new TypeReference<>() {
        });
        return resp.isOk() && Boolean.TRUE.equals(resp.getData());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. RESILIENT EXECUTION ENGINE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Triple-parse fallback for paginated responses:
     * 1. ApiResponse&lt;PageResult&lt;Cheque&gt;&gt; (wrapped)
     * 2. PageResult&lt;Cheque&gt; (raw Spring page)
     * 3. List&lt;Cheque&gt; (plain array → wrapped as single page)
     */
    private ApiResponse<PageResult<Cheque>> executeGetPage(String url) {
        try {
            HttpRequest request = RestApiClient.requestBuilder(url).GET().build();
            HttpResponse<?> response = sendWithRetry(request);
            int status = response.statusCode();
            String body = response.body().toString();

            if (status < 200 || status >= 300) {
                return ApiResponse.error(extractErrorMessage(body, status), status);
            }

            // Strategy 1: Wrapped ApiResponse<PageResult<Cheque>>
            try {
                var type = objectMapper.getTypeFactory()
                        .constructParametricType(ApiResponse.class,
                                objectMapper.getTypeFactory().constructParametricType(PageResult.class, Cheque.class));
                ApiResponse<PageResult<Cheque>> wrapped = objectMapper.readValue(body, type);
                wrapped.setHttpStatus(status);
                return wrapped;
            } catch (Exception e1) {
                // Strategy 2: Raw PageResult<Cheque>
                try {
                    var type = objectMapper.getTypeFactory().constructParametricType(PageResult.class, Cheque.class);
                    PageResult<Cheque> page = objectMapper.readValue(body, type);
                    ApiResponse<PageResult<Cheque>> manual = ApiResponse.ok(page);
                    manual.setHttpStatus(status);
                    return manual;
                } catch (Exception e2) {
                    // Strategy 3: Plain List<Cheque>
                    try {
                        var listType = objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, Cheque.class);
                        List<Cheque> list = objectMapper.readValue(body, listType);
                        PageResult<Cheque> page = new PageResult<>(list, list.size(), 0, list.size());
                        page.setLast(true);
                        ApiResponse<PageResult<Cheque>> manual = ApiResponse.ok(page);
                        manual.setHttpStatus(status);
                        return manual;
                    } catch (Exception e3) {
                        return ApiResponse.error("Parse error: " + e3.getMessage(), status);
                    }
                }
            }
        } catch (Exception e) {
            return logAndWrapError("GET " + url, e);
        }
    }

    private <T> ApiResponse<T> executeGet(String url, TypeReference<T> dataTypeRef) {
        try {
            HttpRequest request = RestApiClient.requestBuilder(url).GET().build();
            HttpResponse<?> response = sendWithRetry(request);
            return parseResponse(response, dataTypeRef);
        } catch (Exception e) {
            return logAndWrapError("GET " + url, e);
        }
    }


    private <T> ApiResponse<T> parseResponse(HttpResponse<?> rawResponse, TypeReference<T> dataTypeRef) {
        int status = rawResponse.statusCode();
        String body = rawResponse.body().toString();

        if (status < 200 || status >= 300) {
            return ApiResponse.error(extractErrorMessage(body, status), status);
        }

        // Strategy 1: Wrapped ApiResponse<T>
        try {
            var dataType = objectMapper.constructType(dataTypeRef.getType());
            var wrappedType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataType);
            ApiResponse<T> wrapped = objectMapper.readValue(body, wrappedType);
            wrapped.setHttpStatus(status);
            return wrapped;
        } catch (Exception wrapEx) {
            // Strategy 2: Raw T
            try {
                T rawData = objectMapper.readValue(body, dataTypeRef);
                ApiResponse<T> manual = ApiResponse.ok(rawData);
                manual.setHttpStatus(status);
                return manual;
            } catch (Exception rawEx) {
                return ApiResponse.error(
                        "Parse error — Wrapped: " + wrapEx.getMessage() + "; Raw: " + rawEx.getMessage(),
                        status);
            }
        }
    }

    private String extractErrorMessage(String body, int status) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("message"))
                return node.get("message").asText();
            if (node.has("error"))
                return node.get("error").asText();
        } catch (Exception ignored) {
        }
        return "HTTP " + status + (body != null && !body.isBlank()
                ? ": " + body.substring(0, Math.min(200, body.length()))
                : "");
    }

    private <T> ApiResponse<T> logAndWrapError(String operation, Exception e) {
        long now = System.currentTimeMillis();
        if (now - LAST_ERROR_LOG_TIME.get() > ERROR_LOG_COOLDOWN_MS) {
            LAST_ERROR_LOG_TIME.set(now);
            System.err.println("[ChequeDAO] Operation '" + operation + "' failed: " + e.getMessage());
        }
        return ApiResponse.error("DAO error [" + operation + "]: " + e.getMessage(), 0);
    }

    private PageResult<Cheque> emptyPage(PageRequest req) {
        return new PageResult<>(Collections.emptyList(), 0, req.getPage(), req.getSize());
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** DTO for /stats/summary endpoint. */
    public record ChequeStats(long total, long pending, long printed, long today, double monthlySum) {
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Thin wrappers — retry now lives in RestApiClient (ResilientHttpClient)
    // ═══════════════════════════════════════════════════════════════════════

    private HttpResponse<?> sendWithRetry(HttpRequest request) throws Exception {
        return RestApiClient.send(request);
    }

    private <T> ApiResponse<T> executeWithRetry(HttpRequest request, TypeReference<T> dataTypeRef) {
        try {
            HttpResponse<?> response = RestApiClient.send(request);
            return parseResponse(response, dataTypeRef);
        } catch (Exception e) {
            return logAndWrapError(request.method() + " " + request.uri(), e);
        }
    }
}