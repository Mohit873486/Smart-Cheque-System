package com.chequeprint.util;

public enum ChequeSizePreset {
    STANDARD("8.0 x 3.66 inches", 8.0, 3.66, "8.0x3.66in"),
    COMPACT("7.5 x 3 inches", 7.5, 3.0, "7.5x3in"),
    A4("A4 Size", 8.27, 11.69, "A4"),
    CUSTOM("Custom size", 8.0, 3.66, "Custom");

    private final String label;
    private final double widthInches;
    private final double heightInches;
    private final String value;

    ChequeSizePreset(String label, double widthInches, double heightInches, String value) {
        this.label = label;
        this.widthInches = widthInches;
        this.heightInches = heightInches;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public double getWidthInches() {
        return widthInches;
    }

    public double getHeightInches() {
        return heightInches;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label;
    }

    public static ChequeSizePreset fromValue(String raw) {
        if (raw == null) {
            return STANDARD;
        }
        for (ChequeSizePreset preset : values()) {
            if (preset.value.equalsIgnoreCase(raw)) {
                return preset;
            }
        }
        return raw.startsWith("CUSTOM:") ? CUSTOM : STANDARD;
    }
}
