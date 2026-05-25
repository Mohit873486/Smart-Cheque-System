package com.chequeprint.util;

import com.chequeprint.model.BankTemplateLayout;

public final class ChequeSizeCodec {

    private ChequeSizeCodec() {
    }

    public static String encode(ChequeSizePreset preset, double widthInches, double heightInches) {
        if (preset == ChequeSizePreset.CUSTOM) {
            return String.format("CUSTOM:%.3fx%.3f", widthInches, heightInches);
        }
        return preset.getValue();
    }

    public static BankTemplateLayout decodeLayout(String rawSize) {
        ChequeSizePreset preset = ChequeSizePreset.fromValue(rawSize);
        if (preset != ChequeSizePreset.CUSTOM) {
            return new BankTemplateLayout(preset.getWidthInches(), preset.getHeightInches());
        }
        double width = 8.5;
        double height = 3.66;
        try {
            String dims = rawSize.substring("CUSTOM:".length());
            String[] parts = dims.toLowerCase().split("x");
            if (parts.length == 2) {
                width = Double.parseDouble(parts[0]);
                height = Double.parseDouble(parts[1]);
            }
        } catch (Exception ignored) {
        }
        return new BankTemplateLayout(width, height);
    }

    public static String display(String rawSize) {
        ChequeSizePreset preset = ChequeSizePreset.fromValue(rawSize);
        if (preset != ChequeSizePreset.CUSTOM) {
            return preset.getLabel();
        }
        BankTemplateLayout layout = decodeLayout(rawSize);
        return String.format("Custom (%.2f x %.2f in)", layout.getWidthInches(), layout.getHeightInches());
    }
}
