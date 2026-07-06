package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditProfileDialogController {

    @FXML
    private TextField tfName;

    @FXML
    private TextField tfEmail;

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnSave;

    private User user;
    private final UserService userService = new UserService();
    private boolean saved = false;

    public void initData(User user) {
        this.user = user;
        if (user != null) {
            tfName.setText(user.getName() != null ? user.getName() : "");
            tfEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        tfName.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 13px;");
        tfEmail.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 13px;");

        String name = tfName.getText().trim();
        String email = tfEmail.getText().trim();

        boolean hasError = false;

        if (name.isEmpty()) {
            tfName.setStyle("-fx-background-color: #fdf2f2; -fx-border-color: #f87171; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 13px;");
            hasError = true;
        }

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (email.isEmpty() || !email.matches(emailRegex)) {
            tfEmail.setStyle("-fx-background-color: #fdf2f2; -fx-border-color: #f87171; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 13px;");
            hasError = true;
        }

        if (hasError) {
            new Alert(Alert.AlertType.WARNING, "Please correct the highlighted fields with valid entries.").show();
            return;
        }

        try {
            user.setName(name);
            user.setEmail(email);

            boolean success = userService.saveProfile(user);
            if (success) {
                saved = true;
                closeStage();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to save profile on backend.").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save profile: " + e.getMessage()).show();
        }
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}
