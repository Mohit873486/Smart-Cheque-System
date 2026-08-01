package com.chequeprint.util;

import javafx.print.Printer;

import java.util.ArrayList;
import java.util.List;

public final class PrinterUtils {

    private PrinterUtils() {
    }

    public static boolean isValidPrinter(Printer printer) {
        return printer != null && printer.getName() != null && !printer.getName().isBlank();
    }

    public static boolean isValidPrinterName(String printerName) {
        return printerName != null && !printerName.isBlank() && findPrinterByName(printerName) != null;
    }

    public static List<Printer> getAllAvailablePrinters() {
        List<Printer> printers = new ArrayList<>();
        try {
            for (Printer printer : Printer.getAllPrinters()) {
                if (isValidPrinter(printer)) {
                    printers.add(printer);
                }
            }
        } catch (Exception e) {
            System.err.println("[Printer] Failed to query system printers: " + e.getMessage());
        }
        return printers;
    }

    public static List<Printer> getValidPrinters() {
        return getAllAvailablePrinters();
    }

    public static List<String> getValidPrinterNames() {
        List<String> names = new ArrayList<>();
        for (Printer printer : getAllAvailablePrinters()) {
            if (!names.contains(printer.getName())) {
                names.add(printer.getName());
            }
        }
        return names;
    }

    public static Printer getDefaultValidPrinter() {
        try {
            Printer defaultPrinter = Printer.getDefaultPrinter();
            if (isValidPrinter(defaultPrinter)) {
                return defaultPrinter;
            }
        } catch (Exception e) {
            System.err.println("[Printer] Failed to query default printer: " + e.getMessage());
        }

        List<Printer> printers = getAllAvailablePrinters();
        return printers.isEmpty() ? null : printers.get(0);
    }

    public static Printer findPrinterByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            for (Printer printer : Printer.getAllPrinters()) {
                if (printer.getName() != null && printer.getName().equalsIgnoreCase(name.trim())) {
                    return printer;
                }
            }
        } catch (Exception e) {
            System.err.println("[Printer] Error searching for printer '" + name + "': " + e.getMessage());
        }
        return null;
    }
}
