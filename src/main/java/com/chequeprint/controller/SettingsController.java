package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SettingsController {
    @FXML private ComboBox<String> cmbDefaultBank;
    @FXML private ComboBox<String> cmbPaperSize;
    @FXML private TextField        fldPrinterName;
    @FXML private CheckBox         chkAutoSave;
    @FXML private CheckBox         chkNotifications;
    @FXML private VBox             rootPane;

    @FXML
    public void initialize() {
        FxUtils.animateIn(rootPane, 0);
        cmbDefaultBank.getItems().addAll("SBI", "HDFC", "ICICI", "Axis Bank");
        cmbDefaultBank.setValue("SBI");
        cmbPaperSize.getItems().addAll("A4", "Letter", "Legal");
        cmbPaperSize.setValue("A4");
        fldPrinterName.setText("HP LaserJet Pro");
        chkAutoSave.setSelected(true);
        chkNotifications.setSelected(true);
    }

    @FXML
    private void onSaveSettings() {
        // Persist to a .properties file or DB settings table
        new Alert(Alert.AlertType.INFORMATION,
            "Settings saved.\n"
            + "Bank: "    + cmbDefaultBank.getValue() + "\n"
            + "Paper: "   + cmbPaperSize.getValue()   + "\n"
            + "Printer: " + fldPrinterName.getText()
        ).show();
    }
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}