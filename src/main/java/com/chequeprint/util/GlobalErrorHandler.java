package com.chequeprint.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

/**
 * Global Error Handler utility for JavaFX.
 * Catches uncaught thread exceptions and formats user-friendly error alerts
 * using Alert(Alert.AlertType.ERROR) categorized into API Failure, Network Error, or Validation Error.
 */
public class GlobalErrorHandler {

    private GlobalErrorHandler() {
        // Utility class
    }

    /**
     * Initializes global uncaught exception handler for JavaFX threads.
     */
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception on thread [" + thread.getName() + "]: " + throwable.getMessage());
            throwable.printStackTrace();
            handleError(throwable);
        });
    }

    /**
     * Handles any Throwable and displays an appropriate styled Alert(AlertType.ERROR).
     * Automatically dispatches to JavaFX Application Thread.
     */
    public static void handleError(Throwable throwable) {
        if (throwable == null) {
            return;
        }

        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.toString();
        Throwable cause = getRootCause(throwable);

        if (isNetworkError(cause, message)) {
            showNetworkError("Could not connect to backend REST server. Please check your network connection or verify that port 8081 is running.\n\nDetails: " + message);
        } else if (isValidationError(cause, message)) {
            showValidationError(message);
        } else if (isApiError(cause, message)) {
            showApiError(message);
        } else {
            showErrorAlert("System Error", "An unexpected error occurred", message);
        }
    }

    /**
     * Displays a Validation Error Alert(AlertType.ERROR).
     */
    public static void showValidationError(String message) {
        showErrorAlert("Validation Error", "Invalid Form Data Provided", message);
    }

    /**
     * Displays an API Failure Alert(AlertType.ERROR).
     */
    public static void showApiError(String message) {
        showErrorAlert("API Failure", "Backend REST API Returned Error", message);
    }

    /**
     * Displays a Network Error Alert(AlertType.ERROR).
     */
    public static void showNetworkError(String message) {
        showErrorAlert("Network Error", "REST Server Communication Failure", message);
    }

    /**
     * General method to show Alert(AlertType.ERROR) dialog on JavaFX thread.
     */
    public static void showErrorAlert(String title, String headerText, String contentText) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showErrorAlert(title, headerText, contentText));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR, contentText, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.showAndWait();
    }

    private static boolean isNetworkError(Throwable cause, String message) {
        if (cause instanceof ConnectException
                || cause instanceof UnknownHostException
                || cause instanceof SocketException
                || cause instanceof HttpTimeoutException) {
            return true;
        }
        if (message != null) {
            String lower = message.toLowerCase();
            return lower.contains("connection refused")
                    || lower.contains("failed to connect")
                    || lower.contains("unknown host")
                    || lower.contains("connect timed out")
                    || lower.contains("network is unreachable");
        }
        return false;
    }

    private static boolean isValidationError(Throwable cause, String message) {
        if (cause instanceof IllegalArgumentException || cause instanceof NumberFormatException) {
            return true;
        }
        if (message != null) {
            String lower = message.toLowerCase();
            return lower.contains("validation")
                    || lower.contains("required")
                    || lower.contains("invalid format")
                    || lower.contains("must be greater than");
        }
        return false;
    }

    private static boolean isApiError(Throwable cause, String message) {
        if (message != null) {
            String lower = message.toLowerCase();
            return lower.contains("http request failed")
                    || lower.contains("http status")
                    || lower.contains("400")
                    || lower.contains("401")
                    || lower.contains("403")
                    || lower.contains("404")
                    || lower.contains("409")
                    || lower.contains("500");
        }
        return false;
    }

    private static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
