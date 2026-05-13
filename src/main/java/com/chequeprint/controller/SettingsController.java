package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SettingsController {

    // ================= BASIC SETTINGS =================
    @FXML private ComboBox<String> cbDefaultBank;
    @FXML private ComboBox<String> cbPaperSize;
    @FXML private ComboBox<String> cbCurrency;
    @FXML private ComboBox<String> cbDateFormat;
    @FXML private ComboBox<String> cbLanguage;
    @FXML private ComboBox<String> cbPaymentTerms;

    @FXML private TextField fldPrinterName;
    @FXML private TextField tfAppName;
    @FXML private TextField tfChequePrefix;
    @FXML private TextField tfInvoicePrefix;

    // ================= CHECKBOX SETTINGS =================
    @FXML private CheckBox chkAutoSave;
    @FXML private CheckBox cbAutoPrint;
    @FXML private CheckBox cbAmountConfirm;
    @FXML private CheckBox cbAutoGST;
    @FXML private CheckBox chkNotifications;

    // ================= THEME =================
    @FXML private ToggleGroup tgTheme;
    @FXML private RadioButton rbLight;
    @FXML private RadioButton rbDark;

    // ================= ROOT =================
    @FXML private VBox rootPane;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        FxUtils.animateIn(rootPane, 0);

        // Bank
        cbDefaultBank.getItems().addAll("SBI", "HDFC", "ICICI", "Axis Bank");
        cbDefaultBank.setValue("SBI");

        // Paper
        cbPaperSize.getItems().addAll("A4", "Letter", "Legal");
        cbPaperSize.setValue("A4");

        // Currency / Format / Language
        cbCurrency.getItems().addAll("INR", "USD", "EUR");
        cbCurrency.setValue("INR");

        cbDateFormat.getItems().addAll("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd");
        cbDateFormat.setValue("dd/MM/yyyy");

        cbLanguage.getItems().addAll("English", "Hindi", "Gujarati");
        cbLanguage.setValue("English");

        cbPaymentTerms.getItems().addAll("Immediate", "7 Days", "15 Days", "30 Days");
        cbPaymentTerms.setValue("Immediate");

        // Defaults
        fldPrinterName.setText("HP LaserJet Pro");
        tfAppName.setText("Smart Cheque System");
        tfChequePrefix.setText("CHQ");
        tfInvoicePrefix.setText("INV");

        chkAutoSave.setSelected(true);
        cbAutoPrint.setSelected(false);
        cbAmountConfirm.setSelected(true);
        cbAutoGST.setSelected(false);
        chkNotifications.setSelected(true);

        rbLight.setSelected(true);
    }

    // ================= SAVE SETTINGS =================
    @FXML
    private void onSaveSettings() {

        String theme = rbLight.isSelected() ? "Light" : "Dark";

        // (Later replace this with Service/DAO saveSettings())
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings Saved");
        alert.setHeaderText("Configuration Updated Successfully");
        alert.setContentText(
                "App Name: " + tfAppName.getText() + "\n" +
                "Bank: " + cbDefaultBank.getValue() + "\n" +
                "Paper: " + cbPaperSize.getValue() + "\n" +
                "Currency: " + cbCurrency.getValue() + "\n" +
                "Printer: " + fldPrinterName.getText() + "\n" +
                "Theme: " + theme
        );
        alert.show();
    }

    // ================= RESET SETTINGS =================
    @FXML
    private void onResetSettings() {

        cbDefaultBank.setValue("SBI");
        cbPaperSize.setValue("A4");
        cbCurrency.setValue("INR");
        cbDateFormat.setValue("dd/MM/yyyy");
        cbLanguage.setValue("English");
        cbPaymentTerms.setValue("Immediate");

        fldPrinterName.setText("HP LaserJet Pro");
        tfAppName.setText("Smart Cheque System");
        tfChequePrefix.setText("CHQ");
        tfInvoicePrefix.setText("INV");

        chkAutoSave.setSelected(true);
        cbAutoPrint.setSelected(false);
        cbAmountConfirm.setSelected(true);
        cbAutoGST.setSelected(false);
        chkNotifications.setSelected(true);

        rbLight.setSelected(true);
    }
}