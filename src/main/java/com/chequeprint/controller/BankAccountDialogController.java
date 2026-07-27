package com.chequeprint.controller;

import com.chequeprint.model.BankAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class BankAccountDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private TextField fldBankName;
    @FXML private TextField fldAccountNumber;
    @FXML private TextField fldHolderName;
    @FXML private TextField fldIfscCode;
    @FXML private Button btnSave;

    private Consumer<BankAccount> onSaveCallback;
    private BankAccount existingAccount;

    public void setOnSaveCallback(Consumer<BankAccount> callback) {
        this.onSaveCallback = callback;
    }

    public void initData(BankAccount account) {
        this.existingAccount = account;
        if (account != null) {
            if (lblTitle != null) lblTitle.setText("Edit Bank Account");
            if (lblSubtitle != null) lblSubtitle.setText("Update details for connected bank account");
            if (btnSave != null) btnSave.setText("Save Changes");

            if (fldBankName != null) fldBankName.setText(account.getBankName() != null ? account.getBankName() : "");
            if (fldAccountNumber != null) fldAccountNumber.setText(account.getAccountNumber() != null ? account.getAccountNumber() : "");
            if (fldHolderName != null) fldHolderName.setText(account.getAccountHolderName() != null ? account.getAccountHolderName() : "");
            if (fldIfscCode != null) fldIfscCode.setText(account.getIfscCode() != null ? account.getIfscCode() : "");
        }
    }

    // Standard Indian Financial System Code (IFSC) Regex: 4 letters, '0', 6 alphanumeric chars
    private static final String IFSC_REGEX = "^[A-Z]{4}0[A-Z0-9]{6}$";
    // Account Number Regex: 9 to 18 numeric digits
    private static final String ACCOUNT_NUM_REGEX = "^[0-9]{9,18}$";

    @FXML
    private void onSave() {
        String bankName = fldBankName.getText() != null ? fldBankName.getText().trim() : "";
        String accountNumber = fldAccountNumber.getText() != null ? fldAccountNumber.getText().trim() : "";
        String holderName = fldHolderName.getText() != null ? fldHolderName.getText().trim() : "";
        String ifscCode = fldIfscCode.getText() != null ? fldIfscCode.getText().trim().toUpperCase() : "";

        // Reset field styling
        resetFieldStyles();

        StringBuilder errors = new StringBuilder();

        // 1. Bank Name Validation
        if (bankName.isEmpty()) {
            errors.append("• Bank Name is required.\n");
            markInvalid(fldBankName);
        } else if (bankName.length() < 2) {
            errors.append("• Bank Name must be at least 2 characters long.\n");
            markInvalid(fldBankName);
        }

        // 2. Account Number Validation
        if (accountNumber.isEmpty()) {
            errors.append("• Account Number is required.\n");
            markInvalid(fldAccountNumber);
        } else if (!accountNumber.matches(ACCOUNT_NUM_REGEX)) {
            errors.append("• Account Number must contain 9 to 18 numeric digits (e.g. 123456789012).\n");
            markInvalid(fldAccountNumber);
        }

        // 3. IFSC Code Format Validation
        if (ifscCode.isEmpty()) {
            errors.append("• IFSC Code is required.\n");
            markInvalid(fldIfscCode);
        } else if (!ifscCode.matches(IFSC_REGEX)) {
            errors.append("• Invalid IFSC Code format (e.g. SBIN0000123). Must be 4 uppercase letters, '0', and 6 alphanumeric characters.\n");
            markInvalid(fldIfscCode);
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return;
        }

        BankAccount account = (existingAccount != null) ? existingAccount : new BankAccount();
        account.setBankName(bankName);
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(holderName);
        account.setIfsc(ifscCode);
        account.setBranch(ifscCode);

        if (onSaveCallback != null) {
            onSaveCallback.accept(account);
        }
        closeStage();
    }

    private void resetFieldStyles() {
        String defaultStyle = "-fx-border-color: #e5e7eb;";
        if (fldBankName != null) fldBankName.setStyle(defaultStyle);
        if (fldAccountNumber != null) fldAccountNumber.setStyle(defaultStyle);
        if (fldHolderName != null) fldHolderName.setStyle(defaultStyle);
        if (fldIfscCode != null) fldIfscCode.setStyle(defaultStyle);
    }

    private void markInvalid(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5; -fx-background-color: #fef2f2;");
        }
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    private void closeStage() {
        if (fldBankName != null && fldBankName.getScene() != null) {
            Stage stage = (Stage) fldBankName.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}
