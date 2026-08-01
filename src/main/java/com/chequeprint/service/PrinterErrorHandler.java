package com.chequeprint.service;

import com.chequeprint.util.PrinterUtils;
import javafx.print.Printer;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized printer failure classification and diagnostics layer.
 *
 * Keeps printer failure handling consistent across preview, service, and print flows.
 */
public final class PrinterErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(PrinterErrorHandler.class.getName());

    public enum FailureType {
        PRINTER_NOT_FOUND,
        PRINTER_OFFLINE,
        JOB_FAILED,
        UNKNOWN
    }

    private PrinterErrorHandler() {
    }

    /**
     * Classifies a printing failure into a specific FailureType enum.
     */
    public static FailureType classify(Printer printer, Throwable cause) {
        return classify(printer, cause, false);
    }

    public static FailureType classify(Printer printer, Throwable cause, boolean isJobFailed) {
        if (printer == null || !PrinterUtils.isValidPrinter(printer)) {
            return FailureType.PRINTER_NOT_FOUND;
        }

        if (isJobFailed) {
            return FailureType.JOB_FAILED;
        }

        String message = cause != null ? cause.getMessage() : null;
        if (message != null && isOfflineSignal(message)) {
            return FailureType.PRINTER_OFFLINE;
        }

        return FailureType.UNKNOWN;
    }

    /**
     * Generates a user-friendly error message with clear recovery guidance.
     */
    public static String buildUserMessage(Printer printer, String action, Throwable cause) {
        return buildUserMessage(printer, action, cause, false);
    }

    public static String buildUserMessage(Printer printer, String action, Throwable cause, boolean isJobFailed) {
        FailureType type = classify(printer, cause, isJobFailed);
        String printerName = printer != null ? printer.getName() : "<Unknown Printer>";

        String message = switch (type) {
            case PRINTER_NOT_FOUND ->
                    "❌ Printer Not Found: Printer '" + printerName + "' is not installed or detected on your system. Please select a valid printer in Printer Settings.";
            case PRINTER_OFFLINE ->
                    "🔌 Printer Offline: Printer '" + printerName + "' appears to be offline. Please verify printer power, USB/Network connection, and paper tray.";
            case JOB_FAILED ->
                    "⚠️ Print Job Failed: The print job sent to '" + printerName + "' failed to spool or complete. Please retry or restart your printer spooler service.";
            default ->
                    "⚠️ Printing Error: Unable to perform '" + action + "' on printer '" + printerName + "'. Details: " + safeErrorText(cause);
        };

        // Log error automatically for debugging
        logFailure(type, printerName, action, cause);
        return message;
        }

    /**
     * Logs detailed failure diagnostics to system logger for troubleshooting.
     */
    public static void logFailure(FailureType type, String printerName, String action, Throwable cause) {
        String logHeader = String.format("[PrinterErrorHandler] Action: '%s' | Target Printer: '%s' | Classification: %s",
                action, printerName, type);
        
        if (cause != null) {
            LOGGER.log(Level.SEVERE, logHeader + " | Exception: " + cause.getMessage(), cause);
        } else {
            LOGGER.log(Level.SEVERE, logHeader);
        }
    }

    private static boolean isOfflineSignal(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        List<String> offlineKeywords = Arrays.asList(
                "offline",
                "not connected",
                "disconnected",
                "unavailable",
                "not available",
                "paper jam",
                "out of paper",
                "spooler error"
        );
        String lowered = message.toLowerCase();
        return offlineKeywords.stream().anyMatch(lowered::contains);
    }

    private static String safeErrorText(Throwable cause) {
        if (cause == null) {
            return "No detailed exception was reported.";
        }
        String message = cause.getMessage();
        return message != null && !message.isBlank() ? message : cause.getClass().getSimpleName();
    }
}
