package com.chequeprint.controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Owns reusable bank-screen dialog and alert UI.
 */
public class BankDialogController {

    protected VBox createStyledFormField(String labelText, String placeholder) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");

        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.setStyle(
                "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 9 12; -fx-font-size: 13px;");

        box.getChildren().addAll(label, field);
        return box;
    }

    protected void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
