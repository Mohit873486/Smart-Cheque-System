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
    @FXML private TextField fldBranchName;
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
            if (fldBranchName != null) fldBranchName.setText(account.getBranchName() != null ? account.getBranchName() : "");
        }
    }

    @FXML
    private void onSave() {
        String bankName = fldBankName.getText() != null ? fldBankName.getText().trim() : "";
        String accountNumber = fldAccountNumber.getText() != null ? fldAccountNumber.getText().trim() : "";
        String holderName = fldHolderName.getText() != null ? fldHolderName.getText().trim() : "";
        String ifscCode = fldIfscCode.getText() != null ? fldIfscCode.getText().trim() : "";
        String branchName = fldBranchName != null && fldBranchName.getText() != null ? fldBranchName.getText().trim() : "";

        if (bankName.isEmpty() || accountNumber.isEmpty() || holderName.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText("Bank Name, Account Number, and Account Holder Name are required.");
            alert.showAndWait();
            return;
        }

        BankAccount account = (existingAccount != null) ? existingAccount : new BankAccount();
        account.setBankName(bankName);
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(holderName);
        account.setIfsc(ifscCode);
        account.setBranch(branchName);
        account.setBranchName(branchName);

        if (onSaveCallback != null) {
            onSaveCallback.accept(account);
        }
        closeStage();
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
