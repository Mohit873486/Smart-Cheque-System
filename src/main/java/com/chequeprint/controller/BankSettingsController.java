package com.chequeprint.controller;

/**
 * Owns reusable bank template size, unit, and numeric configuration helpers.
 */
public class BankSettingsController extends BankDialogController {

    protected double convertFromInches(double inches, String toUnit) {
        if (toUnit == null) {
            return inches;
        }
        return switch (toUnit) {
            case "Millimeters (mm)" -> inches * 25.4;
            case "Centimeters (cm)" -> inches * 2.54;
            case "Pixels (300 DPI)" -> inches * 300.0;
            case "Pixels (72 DPI)" -> inches * 72.0;
            default -> inches;
        };
    }

    protected double convertToInches(double value, String fromUnit) {
        if (fromUnit == null) {
            return value;
        }
        return switch (fromUnit) {
            case "Millimeters (mm)" -> value / 25.4;
            case "Centimeters (cm)" -> value / 2.54;
            case "Pixels (300 DPI)" -> value / 300.0;
            case "Pixels (72 DPI)" -> value / 72.0;
            default -> value;
        };
    }

    protected double parsePositive(String raw, String label) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (Exception ex) {
            throw new IllegalArgumentException(label + " must be a valid number in mm.");
        }
    }

    protected String formatMm(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    protected String safeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    protected static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
