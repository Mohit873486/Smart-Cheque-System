package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.LayoutField;
import com.chequeprint.util.AppState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business Service for Cheque Template processing, ratio coordinate mapping, and persistence.
 * Centralizes layout coordinate logic ($X/Y$ ratios, field mapping) between UI Controllers and DAOs.
 */
public class TemplateService {

    private final BankDAO bankDao;

    public TemplateService() {
        this.bankDao = new BankDAO();
    }

    public TemplateService(BankDAO bankDao) {
        this.bankDao = bankDao;
    }

    /**
     * Fetches template field records from DAO and constructs a normalized BankTemplateLayout.
     */
    public BankTemplateLayout loadTemplateLayout(Long bankId) throws Exception {
        List<Map<String, Object>> templates = bankDao.findTemplatesByBankId(bankId);
        Long targetTemplateId = bankId;
        if (!templates.isEmpty() && templates.get(0).get("id") instanceof Number) {
            targetTemplateId = ((Number) templates.get(0).get("id")).longValue();
        }

        List<Map<String, Object>> fields = bankDao.findTemplateFields(targetTemplateId);
        BankTemplateLayout layout = new BankTemplateLayout();

        if (fields != null && !fields.isEmpty()) {
            for (Map<String, Object> map : fields) {
                String name = (String) map.get("fieldName");
                Object xObj = map.get("xPosition");
                Object yObj = map.get("yPosition");
                if (name != null && xObj instanceof Number && yObj instanceof Number) {
                    double x = ((Number) xObj).doubleValue();
                    double y = ((Number) yObj).doubleValue();
                    LayoutField field = unmapFieldName(name);
                    if (field != null) {
                        layout.setFieldPosition(field, x / 720.0, y / 300.0);
                    }
                }
            }
        }
        layout.ensureAllFields();
        return layout;
    }

    /**
     * Converts layout coordinates into field map payloads and persists via DAO.
     */
    public boolean saveTemplateLayout(Long bankAccountId, BankTemplateLayout layout, double canvasW, double canvasH, String fontFamily, int fontSize) throws Exception {
        if (layout == null || !layout.isValidLayout()) {
            return false;
        }

        List<Map<String, Object>> fieldsPayload = layout.toFieldPayloadList(bankAccountId, canvasW, canvasH, fontFamily, fontSize);
        return bankDao.saveTemplateFields(fieldsPayload);
    }

    public LayoutField unmapFieldName(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "name", "payee" -> LayoutField.PAYEE;
            case "amount", "amount_number" -> LayoutField.AMOUNT_NUMBER;
            case "amount_words" -> LayoutField.AMOUNT_WORDS;
            case "date" -> LayoutField.DATE;
            case "signature" -> LayoutField.SIGNATURE;
            case "logo" -> LayoutField.BANK_LOGO;
            case "micr" -> LayoutField.MICR;
            default -> null;
        };
    }
}
