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

    public static FailureType classify(Printer printer, Throwable cause) {
        return classify(printer, cause, false);
    }

    public static FailureType classify(Printer printer, Throwable cause, boolean jobFailed) {
        if (!PrinterUtils.isValidPrinter(printer)) {
            return FailureType.PRINTER_NOT_FOUND;
        }

        if (jobFailed) {
            return FailureType.JOB_FAILED;
        }

        String message = cause != null ? cause.getMessage() : null;
        if (message != null && isOfflineSignal(message)) {
            return FailureType.PRINTER_OFFLINE;
        }

        return FailureType.UNKNOWN;
    }

    public static String buildUserMessage(Printer printer, String action, Throwable cause) {
        return buildUserMessage(printer, action, cause, false);
    }

    public static String buildUserMessage(Printer printer, String action, Throwable cause, boolean jobFailed) {
        FailureType type = classify(printer, cause, jobFailed);
        String printerName = printer != null ? printer.getName() : "<unknown>";

        String message = switch (type) {
            case PRINTER_NOT_FOUND ->
                    "Printer '" + printerName + "' was not found on the system. Please select a valid printer and try again.";
            case PRINTER_OFFLINE ->
                    "Printer '" + printerName + "' appears to be offline or unavailable. Check the printer connection and retry.";
            case JOB_FAILED ->
                    "The print job for printer '" + printerName + "' failed to start or complete. Please retry or choose another printer.";
            default ->
                    "Unable to " + action + " on printer '" + printerName + "'. " + safeErrorText(cause);
        };

        logFailure(type, printerName, action, cause);
        return message;
    }

    public static void logFailure(FailureType type, String printerName, String action, Throwable cause) {
        String message = "[PrinterErrorHandler] " + action + " failed for printer '" + printerName + "' with type=" + type;
        if (cause != null && cause.getMessage() != null) {
            LOGGER.log(Level.SEVERE, message + ". Cause: " + cause.getMessage(), cause);
        } else {
            LOGGER.log(Level.SEVERE, message);
        }
    }

    private static boolean isOfflineSignal(String message) {
        if (message == null) {
            return false;
        }
        List<String> offlineKeywords = Arrays.asList(
                "offline",
                "not connected",
                "disconnected",
                "unavailable",
                "not available",
                "printer is offline"
        );
        String lowered = message.toLowerCase();
        return offlineKeywords.stream().anyMatch(lowered::contains);
    }

    private static String safeErrorText(Throwable cause) {
        if (cause == null) {
            return "No additional details were provided.";
        }
        String message = cause.getMessage();
        return message != null && !message.isBlank() ? message : cause.getClass().getSimpleName();
    }
}
