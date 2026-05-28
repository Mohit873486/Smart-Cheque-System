package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.service.UserService;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ProfileController {

    // =========================
    // OLD PROFILE FIELDS
    // =========================
    @FXML
    private TextField fldName, fldEmail, fldPhone, fldCompany;
    @FXML
    private TextArea fldAddress;
    @FXML
    private Label lblInitials;
    @FXML
    private VBox rootPane;

    // =========================
    // NEW UI FIELDS (MERGED)
    // =========================
    @FXML
    private Label lblAvatarInitials, lblProfileName, lblProfileRole;

    @FXML
    private Label lblStatCheques, lblStatInvoices, lblStatBanks;

    @FXML
    private TextField tfFirstName, tfLastName, tfEmail, tfPhone, tfRole;
    @FXML
    private TextField tfCompany, tfGST;
    @FXML
    private TextArea taAddress;

    @FXML
    private PasswordField pfCurrentPassword, pfNewPassword, pfConfirmPassword;

    @FXML
    private Button btnSaveProfile, btnCancelProfile, btnSaveBusiness, btnChangePassword;

    private final UserService userService = new UserService();
    private User user;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        if (rootPane != null) {
            FxUtils.animateIn(rootPane, 0);
        }

        loadProfile();
    }

    // =========================
    // LOAD PROFILE DATA
    // =========================
    private void loadProfile() {
        new Thread(() -> {
            try {
                user = userService.loadProfile();

                if (user != null) {
                    Platform.runLater(() -> {

                        // =========================
                        // OLD FIELDS (COMPATIBILITY)
                        // =========================
                        set(fldName, user.getName());
                        set(fldEmail, user.getEmail());
                        set(fldPhone, user.getPhone());
                        set(fldCompany, user.getCompany());
                        set(fldAddress, user.getAddress());

                        // =========================
                        // NEW SPLIT NAME LOGIC
                        // =========================
                        String[] nameParts = splitName(user.getName());
                        set(tfFirstName, nameParts[0]);
                        set(tfLastName, nameParts[1]);

                        set(tfEmail, user.getEmail());
                        set(tfPhone, user.getPhone());
                        set(tfCompany, user.getCompany());

                        // =========================
                        // PROFILE UI HEADER
                        // =========================
                        String initials = UserService.getInitials(user.getName());

                        if (lblInitials != null)
                            lblInitials.setText(initials);
                        if (lblAvatarInitials != null)
                            lblAvatarInitials.setText(initials);
                        if (lblProfileName != null)
                            lblProfileName.setText(user.getName());

                        // default role (if not in DB)
                        if (lblProfileRole != null)
                            lblProfileRole.setText("User");

                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                        "Could not load profile: " + e.getMessage()).show());
            }
        }, "load-profile").start();
    }

    // =========================
    // SAVE PROFILE
    // =========================
    @FXML
    private void onSave() {
        try {
            if (user == null)
                user = new User();

            String fullName = tfFirstName.getText().trim() + " " + tfLastName.getText().trim();

            user.setName(fullName.trim());
            user.setEmail(get(tfEmail, fldEmail));
            user.setPhone(get(tfPhone, fldPhone));
            user.setCompany(get(tfCompany, fldCompany));
            user.setAddress(get(taAddress, fldAddress));

            userService.saveProfile(user);

            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController dashboardController) {
                    dashboardController.reload();
                }
            }

            updateUIAfterSave();

            new Alert(Alert.AlertType.INFORMATION, "Profile saved successfully.").show();

        } catch (IllegalArgumentException iae) {
            FxUtils.shake(rootPane);
            new Alert(Alert.AlertType.WARNING, iae.getMessage()).show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Save failed: " + e.getMessage()).show();
        }
    }

    // =========================
    // BUSINESS PROFILE SAVE
    // =========================
    @FXML
    private void onSaveBusiness() {
        try {
            if (user == null) {
                user = new User();
            }

            String comp = tfCompany == null ? "" : tfCompany.getText().trim();
            String addr = taAddress == null ? "" : taAddress.getText().trim();

            user.setCompany(comp);
            user.setAddress(addr);

            userService.saveProfile(user);

            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController dashboardController) {
                    dashboardController.reload();
                }
            }

            updateUIAfterSave();

            new Alert(Alert.AlertType.INFORMATION, "Business details updated.").show();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    // =========================
    // PASSWORD CHANGE
    // =========================
    @FXML
    private void onChangePassword() {
        try {
            if (user == null || user.getId() == 0) {
                throw new IllegalStateException("User profile not loaded.");
            }

            String current = pfCurrentPassword == null ? "" : pfCurrentPassword.getText();
            String newPass = pfNewPassword == null ? "" : pfNewPassword.getText();
            String confirm = pfConfirmPassword == null ? "" : pfConfirmPassword.getText();

            if (newPass.isBlank() || confirm.isBlank()) {
                throw new IllegalArgumentException("New password cannot be empty.");
            }

            if (!newPass.equals(confirm)) {
                throw new IllegalArgumentException("Passwords do not match");
            }

            userService.changePassword(user.getId(), current, newPass);

            if (pfCurrentPassword != null)
                pfCurrentPassword.clear();
            if (pfNewPassword != null)
                pfNewPassword.clear();
            if (pfConfirmPassword != null)
                pfConfirmPassword.clear();

            new Alert(Alert.AlertType.INFORMATION, "Password changed successfully").show();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    // =========================
    // CANCEL
    // =========================
    @FXML
    private void onCancel() {
        System.out.println("Cancel Clicked");
        loadProfile();
    }

    // =========================
    // HELPERS
    // =========================
    private void updateUIAfterSave() {
        String initials = UserService.getInitials(user.getName());

        if (lblInitials != null)
            lblInitials.setText(initials);
        if (lblAvatarInitials != null)
            lblAvatarInitials.setText(initials);
        if (lblProfileName != null)
            lblProfileName.setText(user.getName());
    }

    private void set(TextField tf, String value) {
        if (tf != null)
            tf.setText(value != null ? value : "");
    }

    private void set(TextArea ta, String value) {
        if (ta != null)
            ta.setText(value != null ? value : "");
    }

    private String get(TextField primary, TextField fallback) {
        if (primary != null && !primary.getText().isEmpty())
            return primary.getText().trim();
        if (fallback != null)
            return fallback.getText().trim();
        return "";
    }

    private String get(TextArea primary, TextArea fallback) {
        if (primary != null && !primary.getText().isEmpty())
            return primary.getText().trim();
        if (fallback != null)
            return fallback.getText().trim();
        return "";
    }

    private String[] splitName(String fullName) {
        if (fullName == null)
            return new String[] { "", "" };
        String[] parts = fullName.trim().split(" ", 2);
        return parts.length == 2 ? parts : new String[] { parts[0], "" };
    }
}