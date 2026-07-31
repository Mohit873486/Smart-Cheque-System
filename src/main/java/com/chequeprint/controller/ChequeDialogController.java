package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.*;
import com.chequeprint.util.AppState;
import com.chequeprint.util.ChequePreviewEngine;
import com.chequeprint.util.PreviewEngine;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChequeDialogController {

    @FXML
    private Label lblFormTitle;
    @FXML
    private TextField fldPayee;
    @FXML
    private TextField fldAmount;
    @FXML
    private HBox moneyInputShell;
    @FXML
    private ComboBox<String> cmbBank;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnSaveAndPrint;

    private Cheque selectedCheque;
    private boolean saved = false;

    private final ChequeService chequeService = new ChequeService();
    private final ChequeWorkflowService workflowService = new ChequeWorkflowService();
    private final BankService bankService = new BankService();
    private final PrintService printService = new PrintService();
    
    private final java.util.Map<String, Integer> bankNameToId = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Bank> bankNameToBank = new java.util.LinkedHashMap<>();

    @FXML
    private Pane chequePreviewCard;
    @FXML
    private Label previewName;
    @FXML
    private Label previewAmount;
    @FXML
    private Label previewAmountWords;
    @FXML
    private Label previewDate;
    @FXML
    private Label previewBank;

    @FXML
    private void initialize() {
        if (fldAmount != null && moneyInputShell != null) {
            fldAmount.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    if (!moneyInputShell.getStyleClass().contains("focused")) {
                        moneyInputShell.getStyleClass().add("focused");
                    }
                } else {
                    moneyInputShell.getStyleClass().remove("focused");
                }
            });
        }
        AppState.getInstance().ensureBankAndTemplateLoaded();
        setupRealTimePreview();
    }

    private void setupRealTimePreview() {
        ChangeListener<Object> listener = (obs, oldVal, newVal) -> updatePreviewEngine();
        if (fldPayee != null) fldPayee.textProperty().addListener(listener);
        if (fldAmount != null) fldAmount.textProperty().addListener(listener);
        if (cmbBank != null) cmbBank.valueProperty().addListener(listener);
        if (datePicker != null) datePicker.valueProperty().addListener(listener);
        AppState.getInstance().addStateChangeListener(this::updatePreviewEngine);
        updatePreviewEngine();
    }

    private boolean updatingPreview = false;

    private void updatePreviewEngine() {
        if (updatingPreview) return;
        updatingPreview = true;
        try {
            if (chequePreviewCard != null) {
                Cheque draft = getDraftChequeFromForm();
                chequePreviewCard.setUserData(null);

                String selectedBankName = cmbBank != null ? cmbBank.getValue() : null;
                Bank activeBank = null;
                if (selectedBankName != null && bankNameToBank.containsKey(selectedBankName)) {
                    activeBank = bankNameToBank.get(selectedBankName);
                } else if (selectedBankName != null && !selectedBankName.isBlank()) {
                    activeBank = new Bank(selectedBankName, selectedBankName.toUpperCase(), "DEFAULT", true);
                } else {
                    activeBank = AppState.getInstance().getSelectedBank();
                }

                if (activeBank != null) {
                    AppState.getInstance().setSelectedBank(activeBank);
                }

                PreviewEngine.render(chequePreviewCard, draft, activeBank, AppState.getInstance().getSelectedTemplate());
            }
        } finally {
            updatingPreview = false;
        }
    }

    private Cheque getDraftChequeFromForm() {
        String payee = fldPayee != null ? fldPayee.getText() : "";
        BigDecimal amt = BigDecimal.ZERO;
        if (fldAmount != null && fldAmount.getText() != null && !fldAmount.getText().isBlank()) {
            try {
                String cleanStr = fldAmount.getText().replaceAll(",", "").trim();
                amt = new BigDecimal(cleanStr);
            } catch (Exception ignored) {}
        }
        LocalDate date = datePicker != null && datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        return new Cheque(null, payee, amt, 1, date);
    }

    public void initData(Cheque cheque) {
        this.selectedCheque = cheque;
        AppState.getInstance().setCurrentCheque(cheque);
        
        loadBanksIntoCombo();
        
        if (cheque != null) {
            lblFormTitle.setText("Edit Cheque");
            fldPayee.setText(cheque.getPayeeName());
            fldAmount.setText(cheque.getAmount() != null ? cheque.getAmount().toPlainString() : "");
            datePicker.setValue(cheque.getIssueDate() != null ? cheque.getIssueDate() : LocalDate.now());
        } else {
            lblFormTitle.setText("New Cheque");
            fldPayee.clear();
            fldAmount.clear();
            datePicker.setValue(LocalDate.now());
        }
        applyPermissions();
    }

    public boolean isSaved() {
        return saved;
    }

    private void applyPermissions() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        boolean canCreate = AccessControl.can(actor, Permission.CREATE_CHEQUE);
        boolean canUpdate = AccessControl.can(actor, Permission.UPDATE_CHEQUE);
        boolean canPrint = AccessControl.can(actor, Permission.PRINT_CHEQUE);
        boolean canEdit = selectedCheque == null ? canCreate : canUpdate;

        btnSave.setVisible(canEdit);
        btnSave.setManaged(canEdit);
        btnSaveAndPrint.setVisible(canPrint);
        btnSaveAndPrint.setManaged(canPrint);

        fldPayee.setEditable(canEdit);
        fldAmount.setEditable(canEdit);
        cmbBank.setDisable(!canEdit);
        datePicker.setDisable(!canEdit);
    }

    private void loadBanksIntoCombo() {
        new Thread(() -> {
            List<Bank> banks = new ArrayList<>();
            try {
                banks = bankService.getAll();
            } catch (Exception ignored) {}

            if (banks == null || banks.isEmpty()) {
                try {
                    List<com.chequeprint.model.BankAccount> accs = new com.chequeprint.service.BankAccountService().fetchAccounts();
                    if (accs != null && !accs.isEmpty()) {
                        banks = new ArrayList<>();
                        for (com.chequeprint.model.BankAccount acc : accs) {
                            if (acc.getBankName() != null && !acc.getBankName().isBlank()) {
                                Bank b = new Bank(acc.getBankName(), acc.getBankName().toUpperCase(), "DEFAULT", true);
                                if (acc.getId() != null) b.setId(acc.getId().intValue());
                                banks.add(b);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (banks == null || banks.isEmpty()) {
                banks = List.of(
                    new Bank("State Bank of India", "SBI", "DEFAULT", true),
                    new Bank("HDFC Bank", "HDFC", "DEFAULT", true),
                    new Bank("ICICI Bank", "ICICI", "DEFAULT", true),
                    new Bank("Axis Bank", "AXIS", "DEFAULT", true),
                    new Bank("Bank of Baroda", "BOB", "DEFAULT", true)
                );
            }

            final List<Bank> finalBanks = banks;
            Platform.runLater(() -> {
                bankNameToId.clear();
                bankNameToBank.clear();
                ObservableList<String> names = FXCollections.observableArrayList();
                int idx = 1;
                for (Bank b : finalBanks) {
                    names.add(b.getBankName());
                    int id = b.getId() != null ? b.getId() : idx++;
                    bankNameToId.put(b.getBankName(), id);
                    bankNameToBank.put(b.getBankName(), b);
                }

                cmbBank.setDisable(false);
                cmbBank.setItems(names);

                Bank appBank = AppState.getInstance().getSelectedBank();
                if (selectedCheque != null && selectedCheque.getBankName() != null && names.contains(selectedCheque.getBankName())) {
                    cmbBank.setValue(selectedCheque.getBankName());
                } else if (appBank != null && appBank.getBankName() != null && names.contains(appBank.getBankName())) {
                    cmbBank.setValue(appBank.getBankName());
                } else if (!names.isEmpty()) {
                    cmbBank.setValue(names.get(0));
                }
            });
        }, "load-banks-dialog").start();
    }

    @FXML
    private void onSave() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, selectedCheque == null ? Permission.CREATE_CHEQUE : Permission.UPDATE_CHEQUE)) {
            showAlert("Permission Denied", "You do not have permission to save cheques.", Alert.AlertType.ERROR);
            return;
        }
        try {
            String payee = fldPayee.getText().trim();
            String amtStr = fldAmount.getText().trim();

            if (payee.isEmpty() || amtStr.isEmpty() || datePicker.getValue() == null) {
                if (payee.isEmpty()) FxUtils.shake(fldPayee);
                if (amtStr.isEmpty()) FxUtils.shake(fldAmount);
                if (datePicker.getValue() == null) FxUtils.shake(datePicker);
                showAlert("Validation", "Payee name, amount, and issue date are required.", Alert.AlertType.WARNING);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amtStr);
            } catch (NumberFormatException nfe) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Enter a valid numeric amount (e.g. 5000.00).", Alert.AlertType.WARNING);
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Amount must be greater than zero.", Alert.AlertType.WARNING);
                return;
            }

            String selectedBankName = cmbBank.getValue();
            Bank activeBank = bankNameToBank.get(selectedBankName);
            int bankId;
            if (activeBank != null && activeBank.getId() != null) {
                bankId = activeBank.getId();
            } else {
                bankId = bankNameToId.getOrDefault(selectedBankName, 1);
            }

            if (selectedCheque == null) {
                Cheque c = new Cheque(null, payee, amount, bankId, datePicker.getValue());
                c.setBankName(selectedBankName);
                workflowService.createPending(c, actor);
                AppState.getInstance().setCurrentCheque(c);
                AppState.getInstance().setCurrentChequeData(c);
                showAlert("Success", "Cheque created and submitted for approval.", Alert.AlertType.INFORMATION);
            } else {
                selectedCheque.setPayeeName(payee);
                selectedCheque.setAmount(amount);
                selectedCheque.setBankId(bankId);
                selectedCheque.setBankName(selectedBankName);
                selectedCheque.setIssueDate(datePicker.getValue());
                if (!chequeService.update(selectedCheque)) {
                    throw new RuntimeException("Could not update cheque.");
                }
                AppState.getInstance().setCurrentCheque(selectedCheque);
                AppState.getInstance().setCurrentChequeData(selectedCheque);
                showAlert("Success", "Cheque updated.", Alert.AlertType.INFORMATION);
            }
            saved = true;
            closeStage();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onSaveAndPrint() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.PRINT_CHEQUE)) {
            showAlert("Permission Denied", "You do not have permission to print cheques.", Alert.AlertType.ERROR);
            return;
        }
        try {
            String payee = fldPayee.getText().trim();
            String amtStr = fldAmount.getText().trim();

            if (payee.isEmpty() || amtStr.isEmpty() || datePicker.getValue() == null) {
                if (payee.isEmpty()) FxUtils.shake(fldPayee);
                if (amtStr.isEmpty()) FxUtils.shake(fldAmount);
                if (datePicker.getValue() == null) FxUtils.shake(datePicker);
                showAlert("Validation", "Payee name, amount, and issue date are required before printing.", Alert.AlertType.WARNING);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amtStr);
            } catch (NumberFormatException nfe) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Enter a valid numeric amount (e.g. 5000.00).", Alert.AlertType.WARNING);
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Amount must be greater than zero.", Alert.AlertType.WARNING);
                return;
            }

            // 1. Get Active Template & 2. Get Current Cheque Data -> 3. Generate Print
            Cheque draft = getDraftChequeFromForm();
            Bank activeBank = AppState.getInstance().getSelectedBank();
            BankTemplateLayout activeTemplate = AppState.getInstance().getSelectedTemplate();
            javafx.stage.Window window = btnSaveAndPrint.getScene() != null ? btnSaveAndPrint.getScene().getWindow() : null;

            boolean printed = printService.printCheque(draft, activeBank, activeTemplate, window);

            if (printed) {
                String selectedBankName = cmbBank.getValue();
                int bankId = bankNameToId.getOrDefault(selectedBankName,
                        Math.max(1, cmbBank.getSelectionModel().getSelectedIndex() + 1));

                if (selectedCheque == null) {
                    Cheque newCheque = new Cheque(null, payee, amount, bankId, datePicker.getValue());
                    workflowService.createPending(newCheque, actor);
                } else {
                    selectedCheque.setPayeeName(payee);
                    selectedCheque.setAmount(amount);
                    selectedCheque.setBankId(bankId);
                    selectedCheque.setIssueDate(datePicker.getValue());
                    chequeService.update(selectedCheque);
                    workflowService.print(selectedCheque.getId(), actor);
                }
                showAlert("Success", "Cheque sent to printer successfully!", Alert.AlertType.INFORMATION);
                saved = true;
                closeStage();
            }
        } catch (Exception e) {
            showAlert("Print Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    @FXML
    private void onClear() {
        if (fldPayee != null) fldPayee.clear();
        if (fldAmount != null) fldAmount.clear();
    }

    private void closeStage() {
        AppState.getInstance().removeStateChangeListener(this::updatePreviewEngine);
        Stage stage = (Stage) lblFormTitle.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
