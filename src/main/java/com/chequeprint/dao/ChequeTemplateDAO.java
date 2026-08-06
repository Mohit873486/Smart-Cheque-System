package com.chequeprint.dao;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.ChequeTemplate;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.chequeprint.util.HttpClientProvider;
import com.chequeprint.util.Session;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ChequeTemplateDAO — REST client for cheque template and layout field operations.
 *
 * Responsibilities:
 *  • Fetch/save {@link ChequeTemplate} metadata via {@code /api/template}.
 *  • Fetch/save per-field layout ratios via {@code /api/template/fields}
 *    (backed by the {@code template_layout_fields} table introduced in V4).
 *  • Reconstruct a {@link BankTemplateLayout} from the relational field rows
 *    so the {@link com.chequeprint.engine.ChequeRenderEngine} receives the
 *    typed ratio model it expects (no more config_json parsing on the client).
 */
public class ChequeTemplateDAO {

    private static final String API_TEMPLATE        = ApiConfig.BASE_URL + "/api/template";
    private static final String API_TEMPLATE_FIELDS = ApiConfig.BASE_URL + "/api/template/fields";

    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;

    public ChequeTemplateDAO() {
        this.httpClient = HttpClientProvider.getClient();  // ✅ shared, no leak
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ── Auth helper ───────────────────────────────────────────────────────────

    private void addAuth(HttpRequest.Builder b) {
        String h = Session.getAuthorizationHeader();
        if (h != null && !h.isBlank()) b.header("Authorization", h);
    }

    // ── Template CRUD ─────────────────────────────────────────────────────────

    /**
     * Returns the {@link ChequeTemplate} for the given bank ID,
     * or {@code null} when not found or the server is unreachable.
     */
    public ChequeTemplate findByBankId(long bankId) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_TEMPLATE + "/bank/" + bankId))
                    .header("Accept", "application/json");
            addAuth(b);
            HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return objectMapper.readValue(resp.body(), ChequeTemplate.class);
            }
        } catch (Exception ex) {
            System.err.println("ChequeTemplateDAO.findByBankId error: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Saves or updates a template.
     * Returns the persisted entity with its server-assigned ID, or {@code null} on failure.
     */
    public ChequeTemplate save(ChequeTemplate template) {
        try {
            String json = objectMapper.writeValueAsString(template);
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_TEMPLATE + "/save"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuth(b);
            HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return objectMapper.readValue(resp.body(), ChequeTemplate.class);
            }
        } catch (Exception ex) {
            System.err.println("ChequeTemplateDAO.save error: " + ex.getMessage());
        }
        return null;
    }

    // ── Layout field CRUD ─────────────────────────────────────────────────────

    /**
     * Returns the raw layout field rows for {@code templateId} as a list of maps,
     * each with keys: {@code fieldName}, {@code xRatio}, {@code yRatio},
     * {@code widthRatio}, {@code heightRatio}.
     *
     * <p>Returns an empty list when no fields exist or the server is unreachable.
     */
    public List<Map<String, Object>> findLayoutFields(long templateId) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_TEMPLATE_FIELDS + "/" + templateId))
                    .header("Accept", "application/json");
            addAuth(b);
            HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return objectMapper.readValue(resp.body(),
                        new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception ex) {
            System.err.println("ChequeTemplateDAO.findLayoutFields error: " + ex.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Saves a list of layout field rows to {@code /api/template/fields}.
     * Each map must contain: {@code templateId}, {@code fieldName}, {@code xPosition},
     * {@code yPosition}, {@code fontSize}, {@code fontFamily}.
     *
     * @return {@code true} on success
     */
    public boolean saveLayoutFields(List<Map<String, Object>> fieldsPayload) {
        try {
            String json = objectMapper.writeValueAsString(fieldsPayload);
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_TEMPLATE_FIELDS))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuth(b);
            HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception ex) {
            System.err.println("ChequeTemplateDAO.saveLayoutFields error: " + ex.getMessage());
            return false;
        }
    }

    // ── BankTemplateLayout reconstruction ─────────────────────────────────────

    /**
     * Fetches the layout fields for {@code templateId} from the server and rebuilds
     * a fully populated {@link BankTemplateLayout} ready for the render engine.
     *
     * <p>Each row in {@code template_layout_fields} maps to one {@link LayoutField}
     * via its {@code field_name} column. Any field missing from the server response
     * retains its hardcoded default from {@link BankTemplateLayout}.
     *
     * <p>Falls back gracefully to a default layout when the server returns no rows
     * (e.g. new template with no customisation yet, or server offline).
     *
     * @param templateId the {@link ChequeTemplate#getId()} value
     * @param widthMm    canvas width in mm  (use {@code ChequeTemplate#getWidth()})
     * @param heightMm   canvas height in mm (use {@code ChequeTemplate#getHeight()})
     * @return a {@link BankTemplateLayout} with all 7 field positions populated
     */
    public BankTemplateLayout loadLayout(long templateId, double widthMm, double heightMm) {
        // Convert physical mm to "inches" — the layout model uses inch dimensions
        double widthInches  = widthMm  / 25.4;
        double heightInches = heightMm / 25.4;

        BankTemplateLayout layout = new BankTemplateLayout(widthInches, heightInches);

        List<Map<String, Object>> rows = findLayoutFields(templateId);
        if (rows.isEmpty()) {
            // No customisation stored — return default layout
            return layout;
        }

        // Build a lookup from fieldName → FieldPosition
        EnumMap<LayoutField, FieldPosition> positions = new EnumMap<>(LayoutField.class);
        for (Map<String, Object> row : rows) {
            String name = stringVal(row, "fieldName");
            if (name == null) name = stringVal(row, "field_name");
            if (name == null) continue;

            try {
                LayoutField field = LayoutField.valueOf(name.toUpperCase());
                double x = doubleVal(row, "xRatio",      doubleVal(row, "x_ratio",      0.0));
                double y = doubleVal(row, "yRatio",      doubleVal(row, "y_ratio",      0.0));
                double w = doubleVal(row, "widthRatio",  doubleVal(row, "width_ratio",  0.0));
                double h = doubleVal(row, "heightRatio", doubleVal(row, "height_ratio", 0.0));
                positions.put(field, new FieldPosition(x, y, w, h));
            } catch (IllegalArgumentException ignored) {
                // Unknown field name — skip
            }
        }

        // Apply fetched positions onto the layout
        for (Map.Entry<LayoutField, FieldPosition> entry : positions.entrySet()) {
            FieldPosition fp = entry.getValue();
            layout.setFieldLayout(entry.getKey(),
                    fp.getXRatio(), fp.getYRatio(),
                    fp.getWidthRatio(), fp.getHeightRatio());
        }

        layout.ensureAllFields();
        return layout;
    }

    /**
     * Convenience overload: fetches the template by bank ID, then loads its layout.
     * Returns a default layout when the template is not found.
     */
    public BankTemplateLayout loadLayoutForBank(long bankId) {
        ChequeTemplate template = findByBankId(bankId);
        if (template == null || template.getId() == null) {
            return new BankTemplateLayout();
        }
        double w = template.getWidth()  != null ? template.getWidth()  : 203.20;
        double h = template.getHeight() != null ? template.getHeight() : 92.00;
        return loadLayout(template.getId(), w, h);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String stringVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static double doubleVal(Map<String, Object> map, String key, double fallback) {
        Object v = map.get(key);
        if (v == null) return fallback;
        try { return Double.parseDouble(v.toString()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
