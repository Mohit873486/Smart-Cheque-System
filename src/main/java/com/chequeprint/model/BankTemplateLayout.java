package com.chequeprint.model;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class BankTemplateLayout implements Serializable {
    private static final long serialVersionUID = 1L;

    private double widthInches;
    private double heightInches;
    private final EnumMap<LayoutField, FieldPosition> fieldPositions = new EnumMap<>(LayoutField.class);

    public BankTemplateLayout() {
        this(8.5, 3.66);
    }

    public BankTemplateLayout(double widthInches, double heightInches) {
        this.widthInches = widthInches;
        this.heightInches = heightInches;
        initDefaults();
    }

    public double getWidthInches() {
        return widthInches;
    }

    public void setWidthInches(double widthInches) {
        this.widthInches = widthInches;
    }

    public double getHeightInches() {
        return heightInches;
    }

    public void setHeightInches(double heightInches) {
        this.heightInches = heightInches;
    }

    public FieldPosition get(LayoutField field) {
        return fieldPositions.computeIfAbsent(field, f -> new FieldPosition(0.5, 0.5));
    }

    public Map<LayoutField, FieldPosition> getFieldPositions() {
        return fieldPositions;
    }

    public void setFieldPosition(LayoutField field, double xRatio, double yRatio) {
        fieldPositions.put(field, new FieldPosition(clamp(xRatio), clamp(yRatio)));
    }

    public BankTemplateLayout copy() {
        BankTemplateLayout copy = new BankTemplateLayout(widthInches, heightInches);
        copy.fieldPositions.clear();
        for (Map.Entry<LayoutField, FieldPosition> entry : fieldPositions.entrySet()) {
            copy.fieldPositions.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public void ensureAllFields() {
        for (LayoutField field : LayoutField.values()) {
            fieldPositions.putIfAbsent(field, defaultPosition(field));
        }
    }

    private void initDefaults() {
        fieldPositions.clear();
        for (LayoutField field : LayoutField.values()) {
            fieldPositions.put(field, defaultPosition(field));
        }
    }

    private static FieldPosition defaultPosition(LayoutField field) {
        return switch (field) {
            case BANK_LOGO -> new FieldPosition(0.08, 0.10);
            case DATE -> new FieldPosition(0.84, 0.20);
            case PAYEE -> new FieldPosition(0.18, 0.38);
            case AMOUNT_NUMBER -> new FieldPosition(0.84, 0.42);
            case AMOUNT_WORDS -> new FieldPosition(0.20, 0.56);
            case SIGNATURE -> new FieldPosition(0.82, 0.82);
            case MICR -> new FieldPosition(0.50, 0.92);
        };
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
