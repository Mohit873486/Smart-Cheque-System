package com.chequeprint.controller;

import com.chequeprint.service.SettingService;
import com.chequeprint.model.Settings;
import com.chequeprint.util.FxUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * SettingsController - Manages application settings, preferences, and
 * configurations
 * 
 * FIXED ISSUES:
 * 1. Added missing @FXML field declarations for all UI elements
 * 2. Added handler methods: onSaveSettings(), onResetSettings()
 * 3. Added MainController reference
 * 4. Added initialization logic for ComboBoxes
 * 5. Added default settings loading and saving
 */
public class SettingsController {

    // ========================================
    // GENERAL SETTINGS FIELDS
    // ========================================
    @FXML
    private TextField tfAppName;
    @FXML
    private ComboBox<String> cbCurrency;
    @FXML
    private ComboBox<String> cbDateFormat;
    @FXML
    private ComboBox<String> cbLanguage;

    // ========================================
    // CHEQUE SETTINGS FIELDS
    // ========================================
    @FXML
    private TextField tfChequePrefix;
    @FXML
    private ComboBox<String> cbDefaultBank;
    @FXML
    private CheckBox cbAutoPrint;
    @FXML
    private CheckBox cbAmountConfirm;

    // ========================================
    // INVOICE SETTINGS FIELDS
    // ========================================
    @FXML
    private TextField tfInvoicePrefix;
    @FXML
    private ComboBox<String> cbPaymentTerms;
    @FXML
    private CheckBox cbAutoGST;

    // ========================================
    // APPEARANCE SETTINGS FIELDS
    // ========================================
    @FXML
    private ToggleGroup tgTheme;
    @FXML
    private RadioButton rbLight;
    @FXML
    private RadioButton rbDark;

    // ========================================
    // ACTION BUTTONS
    // ========================================
    @FXML
    private Button btnSaveSettings;
    @FXML
    private Button btnResetSettings;

    // ========================================
    // SIGNATURE AUTO SETTINGS
    // ========================================
    @FXML
    private javafx.scene.image.ImageView ivSignature;
    @FXML
    private Button btnUploadSignature;
    @FXML
    private Button btnRemoveSignature;
    @FXML
    private CheckBox cbAutoSignature;
    @FXML
    private TextField tfSigWidth;
    @FXML
    private TextField tfSigHeight;
    @FXML
    private TextField tfSigX;
    @FXML
    private TextField tfSigY;

    @FXML
    private Label lblStatusMessage;

    private MainController mainController;
    private final SettingService settingService = new SettingService();

    private void showStatusMessage(String message, boolean isError) {
        if (lblStatusMessage == null) return;
        
        javafx.application.Platform.runLater(() -> {
            lblStatusMessage.setText(message);
            if (isError) {
                lblStatusMessage.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c; -fx-padding: 12 16; -fx-background-radius: 8; -fx-border-color: #fecaca; -fx-border-radius: 8; -fx-font-size: 13px; -fx-font-weight: 500;");
            } else {
                lblStatusMessage.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #047857; -fx-padding: 12 16; -fx-background-radius: 8; -fx-border-color: #a7f3d0; -fx-border-radius: 8; -fx-font-size: 13px; -fx-font-weight: 500;");
            }
            lblStatusMessage.setVisible(true);
            lblStatusMessage.setManaged(true);
        });

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
        delay.setOnFinished(e -> {
            javafx.application.Platform.runLater(() -> {
                lblStatusMessage.setVisible(false);
                lblStatusMessage.setManaged(false);
            });
        });
        delay.play();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        System.out.println("[SettingsController] Initializing...");

        try {
            // Initialize ComboBox options
            initializeCurrencies();
            initializeDateFormats();
            initializeLanguages();
            initializePaymentTerms();

            // Populate cbDefaultBank dropdown asynchronously
            javafx.concurrent.Task<java.util.List<com.chequeprint.model.Bank>> bankTask = new javafx.concurrent.Task<>() {
                @Override
                protected java.util.List<com.chequeprint.model.Bank> call() throws Exception {
                    com.chequeprint.service.BankService bankService = new com.chequeprint.service.BankService();
                    return bankService.getAll();
                }
            };
            bankTask.setOnSucceeded(e -> {
                java.util.List<com.chequeprint.model.Bank> banks = bankTask.getValue();
                javafx.collections.ObservableList<String> bankNames = FXCollections.observableArrayList();
                for (com.chequeprint.model.Bank b : banks) {
                    bankNames.add(b.getBankName());
                }
                cbDefaultBank.setItems(bankNames);
                
                // Load current settings from database/REST API AFTER banks are loaded
                loadSettings();
            });
            bankTask.setOnFailed(e -> {
                System.err.println("[Settings] Error loading banks list: " + bankTask.getException().getMessage());
                // Fallback to load settings even if bank list fetch failed
                loadSettings();
            });

            // Load signature preview if available (local and safe)
            try {
                javafx.scene.image.Image img = com.chequeprint.util.SignatureService.loadSignatureImage();
                if (img != null && ivSignature != null) {
                    ivSignature.setImage(img);
                }
                java.util.Properties meta = com.chequeprint.util.SignatureService.loadMetadata();
                cbAutoSignature.setSelected(Boolean.parseBoolean(meta.getProperty("enabled", "true")));
                tfSigWidth.setText(meta.getProperty("width", "120px"));
                tfSigHeight.setText(meta.getProperty("height", "40px"));
                tfSigX.setText(meta.getProperty("x", "0"));
                tfSigY.setText(meta.getProperty("y", "0"));
            } catch (Exception ignored) {
            }

            Thread bt = new Thread(bankTask);
            bt.setDaemon(true);
            bt.start();

            System.out.println("[SettingsController] Initialization completed successfully");
        } catch (Exception e) {
            System.err.println("[SettingsController] Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================
    // INITIALIZATION METHODS
    // ========================================

    private void initializeCurrencies() {
        cbCurrency.setItems(FXCollections.observableArrayList(
                "₹ Indian Rupee (INR)",
                "$ US Dollar (USD)",
                "€ Euro (EUR)",
                "£ British Pound (GBP)",
                "¥ Japanese Yen (JPY)"));
        cbCurrency.setValue("₹ Indian Rupee (INR)");
    }

    private void initializeDateFormats() {
        cbDateFormat.setItems(FXCollections.observableArrayList(
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd",
                "dd-MMM-yyyy",
                "EEEE, MMM d, yyyy"));
        cbDateFormat.setValue("dd/MM/yyyy");
    }

    private void initializeLanguages() {
        cbLanguage.setItems(FXCollections.observableArrayList(
                "English (India)",
                "English (US)",
                "Hindi",
                "Gujarati",
                "Marathi"));
        cbLanguage.setValue("English (India)");
    }

    private void initializePaymentTerms() {
        cbPaymentTerms.setItems(FXCollections.observableArrayList(
                "Immediate",
                "Net 7",
                "Net 15",
                "Net 30",
                "Net 45",
                "Net 60",
                "Due on Receipt"));
        cbPaymentTerms.setValue("Net 30");
    }

    private void setControlsDisabled(boolean disabled) {
        tfAppName.setDisable(disabled);
        cbCurrency.setDisable(disabled);
        cbDateFormat.setDisable(disabled);
        cbLanguage.setDisable(disabled);
        tfChequePrefix.setDisable(disabled);
        cbDefaultBank.setDisable(disabled);
        cbAutoPrint.setDisable(disabled);
        cbAmountConfirm.setDisable(disabled);
        tfInvoicePrefix.setDisable(disabled);
        cbPaymentTerms.setDisable(disabled);
        cbAutoGST.setDisable(disabled);
        if (rbLight != null) rbLight.setDisable(disabled);
        if (rbDark != null) rbDark.setDisable(disabled);
        btnSaveSettings.setDisable(disabled);
        btnResetSettings.setDisable(disabled);
        if (btnUploadSignature != null) btnUploadSignature.setDisable(disabled);
        if (btnRemoveSignature != null) btnRemoveSignature.setDisable(disabled);
        if (cbAutoSignature != null) cbAutoSignature.setDisable(disabled);
        if (tfSigWidth != null) tfSigWidth.setDisable(disabled);
        if (tfSigHeight != null) tfSigHeight.setDisable(disabled);
        if (tfSigX != null) tfSigX.setDisable(disabled);
        if (tfSigY != null) tfSigY.setDisable(disabled);
    }

    // ========================================
    // LOAD SETTINGS FROM PREFERENCES
    // ========================================

    private void loadSettings() {
        setControlsDisabled(true);
        btnSaveSettings.setText("Loading...");
        showStatusMessage("Loading settings from server...", false);

        javafx.concurrent.Task<Settings> task = new javafx.concurrent.Task<>() {
            @Override
            protected Settings call() throws Exception {
                return settingService.getSettings();
            }
        };

        task.setOnSucceeded(event -> {
            Settings s = task.getValue();
            if (s != null) {
                // General Settings
                tfAppName.setText(s.getAppName() != null ? s.getAppName() : "ChequePro");
                cbCurrency.setValue(s.getCurrency() != null ? s.getCurrency() : "₹ Indian Rupee (INR)");
                cbDateFormat.setValue(s.getDateFormat() != null ? s.getDateFormat() : "dd/MM/yyyy");
                cbLanguage.setValue(s.getLanguage() != null ? s.getLanguage() : "English (India)");

                // Cheque Settings
                tfChequePrefix.setText(s.getChequePrefix() != null ? s.getChequePrefix() : "CHQ-");
                cbDefaultBank.setValue(s.getDefaultBank());
                cbAutoPrint.setSelected(s.isAutoPrint());
                cbAmountConfirm.setSelected(s.isAmountConfirm());

                // Invoice Settings
                tfInvoicePrefix.setText(s.getInvoicePrefix() != null ? s.getInvoicePrefix() : "INV-");
                cbPaymentTerms.setValue(s.getPaymentTerms() != null ? s.getPaymentTerms() : "Net 30");
                cbAutoGST.setSelected(s.isAutoGST());

                // Appearance
                if ("dark".equalsIgnoreCase(s.getTheme())) {
                    if (rbDark != null) rbDark.setSelected(true);
                } else {
                    if (rbLight != null) rbLight.setSelected(true);
                }

                System.out.println("[Settings] Configuration loaded from REST API");
                showStatusMessage("Settings loaded successfully.", false);

                // Update theme dynamically on load
                if (btnSaveSettings.getScene() != null) {
                    com.chequeprint.util.ThemeManager.applyTheme(btnSaveSettings.getScene(), s.getTheme());
                }
            } else {
                loadDefaultFormFields();
                showStatusMessage("No settings found on server. Defaults loaded.", false);
            }
            setControlsDisabled(false);
            btnSaveSettings.setText("Save Settings");
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            String details = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            System.err.println("[Settings] Error loading settings: " + details);
            loadDefaultFormFields();
            setControlsDisabled(false);
            btnSaveSettings.setText("Save Settings");
            showStatusMessage("Connection offline. Loaded local defaults.", true);
            showAlert(Alert.AlertType.ERROR, "Connection Error",
                    "Failed to retrieve settings from REST server (http://localhost:8081/api/settings).\n" +
                    "Details: " + details + "\nLocal fallback defaults have been loaded.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadDefaultFormFields() {
        tfAppName.setText("ChequePro");
        cbCurrency.setValue("₹ Indian Rupee (INR)");
        cbDateFormat.setValue("dd/MM/yyyy");
        cbLanguage.setValue("English (India)");

        tfChequePrefix.setText("CHQ-");
        cbDefaultBank.setValue(null);
        cbAutoPrint.setSelected(false);
        cbAmountConfirm.setSelected(true);

        tfInvoicePrefix.setText("INV-");
        cbPaymentTerms.setValue("Net 30");
        cbAutoGST.setSelected(true);

        if (rbLight != null) rbLight.setSelected(true);
        System.out.println("[Settings] Loaded local fallback default values");
    }

    // ========================================
    // HANDLER: SAVE SETTINGS
    // ========================================

    @FXML
    private void onSaveSettings() {
        try {
            String appName = tfAppName.getText().trim();
            String currency = cbCurrency.getValue();
            String dateFormat = cbDateFormat.getValue();
            String language = cbLanguage.getValue();
            String chequePrefix = tfChequePrefix.getText().trim();
            String defaultBank = cbDefaultBank.getValue();
            boolean autoPrint = cbAutoPrint.isSelected();
            boolean amountConfirm = cbAmountConfirm.isSelected();
            String invoicePrefix = tfInvoicePrefix.getText().trim();
            String paymentTerms = cbPaymentTerms.getValue();
            boolean autoGST = cbAutoGST.isSelected();
            String theme = (rbDark != null && rbDark.isSelected()) ? "dark" : "light";

            Settings s = new Settings(
                    appName, currency, dateFormat,
                    language, chequePrefix, defaultBank,
                    autoPrint, amountConfirm, invoicePrefix,
                    paymentTerms, autoGST, theme);

            setControlsDisabled(true);
            btnSaveSettings.setText("Saving...");
            showStatusMessage("Saving settings to server...", false);

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    settingService.saveSettings(s);

                    // persist signature metadata
                    try {
                        java.util.Properties meta = com.chequeprint.util.SignatureService.loadMetadata();
                        meta.setProperty("enabled", Boolean.toString(cbAutoSignature.isSelected()));
                        meta.setProperty("width", tfSigWidth.getText() == null ? "120px" : tfSigWidth.getText());
                        meta.setProperty("height", tfSigHeight.getText() == null ? "40px" : tfSigHeight.getText());
                        meta.setProperty("x", tfSigX.getText() == null ? "0" : tfSigX.getText());
                        meta.setProperty("y", tfSigY.getText() == null ? "0" : tfSigY.getText());
                        com.chequeprint.util.SignatureService.saveMetadata(meta);
                    } catch (Exception ignored) {
                    }
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                setControlsDisabled(false);
                btnSaveSettings.setText("Save Settings");
                showStatusMessage("Settings saved successfully!", false);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Settings saved successfully!");

                // Update theme dynamically on save (utilizing outer scope 'theme')
                if (btnSaveSettings.getScene() != null) {
                    com.chequeprint.util.ThemeManager.applyTheme(btnSaveSettings.getScene(), theme);
                }
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();
                String details = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                setControlsDisabled(false);
                btnSaveSettings.setText("Save Settings");
                showStatusMessage("Failed to save settings.", true);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings: " + details);
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ========================================
    // HANDLER: RESET TO DEFAULTS
    // ========================================

    @FXML
    private void onResetSettings() {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Reset Settings");
            confirmation.setHeaderText("Reset to Default Settings?");
            confirmation.setContentText("This will restore all settings to their default values. Continue?");

            ButtonType result = confirmation.showAndWait().orElse(ButtonType.CANCEL);

            if (result == ButtonType.OK) {
                // Reset all fields to defaults
                tfAppName.setText("ChequePro");
                cbCurrency.setValue("₹ Indian Rupee (INR)");
                cbDateFormat.setValue("dd/MM/yyyy");
                cbLanguage.setValue("English (India)");

                tfChequePrefix.setText("CHQ-");
                cbAutoPrint.setSelected(false);
                cbAmountConfirm.setSelected(true);

                tfInvoicePrefix.setText("INV-");
                cbPaymentTerms.setValue("Net 30");
                cbAutoGST.setSelected(true);

                rbLight.setSelected(true);

                // TODO: Clear preferences/database settings
                // SettingsService.resetToDefaults()

                showAlert(Alert.AlertType.INFORMATION, "Reset Complete",
                        "All settings have been reset to their default values.");

                System.out.println("[Settings] Reset to defaults completed");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to reset settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    @FXML
    private void onUploadSignature() {
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Upload Signature (PNG)");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PNG Images", "*.png"));
            Stage stage = (Stage) btnSaveSettings.getScene().getWindow();
            java.io.File file = chooser.showOpenDialog(stage);
            if (file == null) return;

            com.chequeprint.util.SignatureService.validateSignatureImage(file);

            // Save to app directory
            com.chequeprint.util.SignatureService.saveSignature(file);
            javafx.scene.image.Image img = com.chequeprint.util.SignatureService.loadSignatureImage();
            if (ivSignature != null && img != null) {
                ivSignature.setImage(img);
            }
            java.util.Properties meta = com.chequeprint.util.SignatureService.loadMetadata();
            meta.setProperty("enabled", Boolean.toString(true));
            com.chequeprint.util.SignatureService.saveMetadata(meta);
            showAlert(Alert.AlertType.INFORMATION, "Signature", "Signature uploaded successfully.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Signature Error", "Failed to upload signature: " + ex.getMessage());
        }
    }

    @FXML
    private void onRemoveSignature() {
        try {
            com.chequeprint.util.SignatureService.removeSignature();
            if (ivSignature != null) ivSignature.setImage(null);
            java.util.Properties meta = com.chequeprint.util.SignatureService.loadMetadata();
            meta.setProperty("enabled", "false");
            com.chequeprint.util.SignatureService.saveMetadata(meta);
            showAlert(Alert.AlertType.INFORMATION, "Signature", "Signature removed.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Signature Error", "Failed to remove signature: " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
