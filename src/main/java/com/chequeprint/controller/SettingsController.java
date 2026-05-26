package com.chequeprint.controller;

import com.chequeprint.dao.SettingDAO;
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

    private MainController mainController;

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

            // Load current settings from preferences/database
            loadSettings();

            // Load signature preview if available
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

    // ========================================
    // LOAD SETTINGS FROM PREFERENCES
    // ========================================

    private void loadSettings() {
        try {
            // General Settings
            tfAppName.setText("ChequePro");
            cbCurrency.setValue("₹ Indian Rupee (INR)");
            cbDateFormat.setValue("dd/MM/yyyy");
            cbLanguage.setValue("English (India)");

            // Cheque Settings
            tfChequePrefix.setText("CHQ-");
            cbAutoPrint.setSelected(false);
            cbAmountConfirm.setSelected(true);

            // Invoice Settings
            tfInvoicePrefix.setText("INV-");
            cbPaymentTerms.setValue("Net 30");
            cbAutoGST.setSelected(true);

            // Appearance
            if (rbLight != null)
                rbLight.setSelected(true);

            System.out.println("[Settings] Default settings loaded");
        } catch (Exception e) {
            System.err.println("[Settings] Error loading settings: " + e.getMessage());
        }
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
            String invoicePrefix = tfInvoicePrefix.getText().trim();
            String theme = rbDark.isSelected() ? "dark" : "light";

            Settings s = new Settings(
                    appName, currency, dateFormat,
                    language, chequePrefix,
                    invoicePrefix, theme);

            // 🔥 DAO CALL
            SettingDAO dao = new SettingDAO();
            dao.saveSettings(s);

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

            showAlert(Alert.AlertType.INFORMATION, "Success", "Settings saved successfully!");

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

            // Save to app directory
            java.nio.file.Path saved = com.chequeprint.util.SignatureService.saveSignature(file);
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