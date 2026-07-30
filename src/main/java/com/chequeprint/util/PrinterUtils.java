package com.chequeprint.util;

import javafx.print.Printer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PrinterUtils {

    private static final List<String> INVALID_PATTERNS = Arrays.asList(
            "fax",
            "pdf",
            "xps",
            "onenote",
            "virtual",
            "document writer",
            "root print queue",
            "foxit",
            "dopdf",
            "cutepdf"
    );

    private PrinterUtils() {
        // Utility class constructor
    }

    public static boolean isValidPrinter(Printer printer) {
        if (printer == null) {
            return false;
        }
        return isValidPrinterName(printer.getName());
    }

    public static boolean isValidPrinterName(String printerName) {
        if (printerName == null || printerName.isBlank()) {
            return false;
        }
        String nameLower = printerName.toLowerCase(Locale.ROOT);
        for (String pattern : INVALID_PATTERNS) {
            if (nameLower.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    public static List<Printer> getValidPrinters() {
        List<Printer> validPrinters = new ArrayList<>();
        try {
            for (Printer printer : Printer.getAllPrinters()) {
                if (isValidPrinter(printer)) {
                    validPrinters.add(printer);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to query system printers: " + e.getMessage());
        }
        return validPrinters;
    }

    public static List<String> getValidPrinterNames() {
        List<String> names = new ArrayList<>();
        for (Printer p : getValidPrinters()) {
            if (p.getName() != null && !p.getName().isBlank() && !names.contains(p.getName())) {
                names.add(p.getName());
            }
        }
        if (names.isEmpty()) {
            try {
                for (Printer p : Printer.getAllPrinters()) {
                    if (p.getName() != null && !p.getName().isBlank() && !names.contains(p.getName())) {
                        names.add(p.getName());
                    }
                }
            } catch (Exception ignored) {}
        }
        return names;
    }

    public static Printer getDefaultValidPrinter() {
        // Do not auto-select OS default printer — force explicit user selection from AppState
        return null;
    }

    public static Printer findPrinterByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            for (Printer p : Printer.getAllPrinters()) {
                if (p.getName() != null && p.getName().equalsIgnoreCase(name.trim())) {
                    return p;
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching for printer '" + name + "': " + e.getMessage());
        }
        return null;
    }
}
