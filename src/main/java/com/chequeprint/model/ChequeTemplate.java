package com.chequeprint.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChequeTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long bankId;
    private String templateName;
    private Double width = 203.20;  // Standard cheque width in mm (8 inches)
    private Double height = 92.00;   // Standard cheque height in mm (3.625 inches)
    private String configJson;

    // Single source of truth for field coordinates, font size, and visibility
    private FieldConfig dateField = new FieldConfig(520.0, 28.0, 13.0, true);
    private FieldConfig payeeField = new FieldConfig(95.0, 72.0, 13.0, true);
    private FieldConfig amountWordsField = new FieldConfig(95.0, 108.0, 12.0, true);
    private FieldConfig amountNumField = new FieldConfig(530.0, 122.0, 14.0, true);
    private FieldConfig bearerField = new FieldConfig(580.0, 72.0, 12.0, true);
    private FieldConfig acPayeeField = new FieldConfig(40.0, 25.0, 14.0, true);
    private FieldConfig signatureField = new FieldConfig(530.0, 160.0, 12.0, true);
    private FieldConfig micrField = new FieldConfig(200.0, 180.0, 12.0, true);

    public ChequeTemplate() {}

    public ChequeTemplate(Long bankId, String templateName) {
        this.bankId = bankId;
        this.templateName = templateName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public FieldConfig getDateField() { return dateField; }
    public void setDateField(FieldConfig dateField) { this.dateField = dateField; }

    public FieldConfig getPayeeField() { return payeeField; }
    public void setPayeeField(FieldConfig payeeField) { this.payeeField = payeeField; }

    public FieldConfig getAmountWordsField() { return amountWordsField; }
    public void setAmountWordsField(FieldConfig amountWordsField) { this.amountWordsField = amountWordsField; }

    public FieldConfig getAmountNumField() { return amountNumField; }
    public void setAmountNumField(FieldConfig amountNumField) { this.amountNumField = amountNumField; }

    public FieldConfig getBearerField() { return bearerField; }
    public void setBearerField(FieldConfig bearerField) { this.bearerField = bearerField; }

    public FieldConfig getAcPayeeField() { return acPayeeField; }
    public void setAcPayeeField(FieldConfig acPayeeField) { this.acPayeeField = acPayeeField; }

    public FieldConfig getSignatureField() { return signatureField; }
    public void setSignatureField(FieldConfig signatureField) { this.signatureField = signatureField; }

    public FieldConfig getMicrField() { return micrField; }
    public void setMicrField(FieldConfig micrField) { this.micrField = micrField; }

    // Inner class representing individual field configuration (x, y, fontSize, visibility)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        private double x;
        private double y;
        private double fontSize = 12.0;
        private String fontFamily = "Arial";
        private boolean bold = false;
        private boolean italic = false;
        private boolean visible = true;

        public FieldConfig() {}

        public FieldConfig(double x, double y, double fontSize, boolean visible) {
            this.x = x;
            this.y = y;
            this.fontSize = fontSize;
            this.visible = visible;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getFontSize() { return fontSize; }
        public void setFontSize(double fontSize) { this.fontSize = fontSize; }

        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }

        public boolean isItalic() { return italic; }
        public void setItalic(boolean italic) { this.italic = italic; }

        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
    }
}
