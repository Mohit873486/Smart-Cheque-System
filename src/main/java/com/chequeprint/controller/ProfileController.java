package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.model.AuditLog;
import com.chequeprint.service.UserService;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

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
    private Label lblAvatarInitials, lblProfileName, lblProfileRole, lblLastLogin;
    @FXML
    private javafx.scene.shape.Circle avatarCircle;

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

    @FXML
    private TableView<AuditLog> tblActivity;
    @FXML
    private TableColumn<AuditLog, String> colAction;
    @FXML
    private TableColumn<AuditLog, String> colDetails;
    @FXML
    private TableColumn<AuditLog, String> colTimestamp;

    private final UserService userService = new UserService();
    private final com.chequeprint.service.ChequeService chequeService = new com.chequeprint.service.ChequeService();
    private final com.chequeprint.service.InvoiceService invoiceService = new com.chequeprint.service.InvoiceService();
    private final com.chequeprint.service.BankService bankService = new com.chequeprint.service.BankService();
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

        setupActivityTable();
        loadProfile();
    }

    // =========================
    // LOAD PROFILE DATA
    // =========================
    private void loadProfile() {
        new Thread(() -> {
            try {
                user = userService.loadProfile();

                int chequesCount = 0;
                try {
                    chequesCount = chequeService.getAll().size();
                } catch (Exception ignored) {}

                int invoicesCount = 0;
                try {
                    invoicesCount = invoiceService.getAll().size();
                } catch (Exception ignored) {}

                int banksCount = 0;
                try {
                    banksCount = bankService.getAll().size();
                } catch (Exception ignored) {}

                List<AuditLog> activityList = Collections.emptyList();
                if (user != null) {
                    try {
                        activityList = userService.loadUserActivity(user.getId());
                    } catch (Exception ignored) {}
                }

                LocalDateTime lastLoginTime = null;
                for (AuditLog log : activityList) {
                    if ("LOGIN".equalsIgnoreCase(String.valueOf(log.getAction()))) {
                        lastLoginTime = log.getCreatedAt();
                        break;
                    }
                }

                final int finalCheques = chequesCount;
                final int finalInvoices = invoicesCount;
                final int finalBanks = banksCount;
                final List<AuditLog> finalActivity = activityList;
                final LocalDateTime finalLastLogin = lastLoginTime;

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
                        set(tfRole, user.getRole());

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

                        if (lblProfileRole != null)
                            lblProfileRole.setText(user.getRole() != null ? user.getRole() : "User");

                        if (lblLastLogin != null) {
                            if (finalLastLogin != null) {
                                lblLastLogin.setText("Last Login: " + finalLastLogin.toString().replace("T", " "));
                            } else {
                                lblLastLogin.setText("Last Login: N/A");
                            }
                        }

                        if (lblStatCheques != null)
                            lblStatCheques.setText(String.valueOf(finalCheques));
                        if (lblStatInvoices != null)
                            lblStatInvoices.setText(String.valueOf(finalInvoices));
                        if (lblStatBanks != null)
                            lblStatBanks.setText(String.valueOf(finalBanks));

                        if (tblActivity != null) {
                            tblActivity.setItems(FXCollections.observableArrayList(finalActivity));
                        }

                        updateAvatarImage();
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                        "Could not load profile: " + e.getMessage()).show());
            }
        }, "load-profile").start();
    }

    private void setupActivityTable() {
        if (colAction != null) {
            colAction.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getAction() != null ? cellData.getValue().getAction().name() : ""));
        }
        if (colDetails != null) {
            colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        }
        if (colTimestamp != null) {
            colTimestamp.setCellValueFactory(cellData -> {
                LocalDateTime dt = cellData.getValue().getCreatedAt();
                return new SimpleStringProperty(dt != null ? dt.toString().replace("T", " ") : "");
            });
        }
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

    private java.io.File getProfileImageFile(int userId) {
        java.io.File dir = new java.io.File(System.getProperty("user.home") + "/.chequeprint/profiles");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new java.io.File(dir, userId + ".png");
    }

    private void updateAvatarImage() {
        if (user == null) return;
        java.io.File imageFile = getProfileImageFile(user.getId());
        if (imageFile.exists()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(imageFile.toURI().toString());
                avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                if (lblAvatarInitials != null) {
                    lblAvatarInitials.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            avatarCircle.setFill(javafx.scene.paint.Color.web("#2563eb"));
            if (lblAvatarInitials != null) {
                lblAvatarInitials.setVisible(true);
            }
        }
    }

    @FXML
    private void onUploadAvatar() {
        if (user == null || user.getId() == 0) {
            new Alert(Alert.AlertType.WARNING, "Please wait until profile is loaded.").show();
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            try {
                java.io.File targetFile = getProfileImageFile(user.getId());
                java.nio.file.Files.copy(selectedFile.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                updateAvatarImage();
                new Alert(Alert.AlertType.INFORMATION, "Profile image updated successfully.").show();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Failed to copy image: " + e.getMessage()).show();
            }
        }
    }
}