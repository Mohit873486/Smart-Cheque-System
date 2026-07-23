package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.*;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    
    private final java.util.Map<String, Integer> bankNameToId = new java.util.LinkedHashMap<>();

    @FXML
    private VBox chequePreviewCard;
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
        setupRealTimePreview();
    }

    private void setupRealTimePreview() {
        // 1. Payee Name binding (fldPayee -> previewName)
        if (fldPayee != null) {
            fldPayee.textProperty().addListener((obs, oldVal, newVal) -> {
                if (previewName != null) {
                    if (newVal == null || newVal.isBlank()) {
                        previewName.setText("PAYEE NAME");
                    } else {
                        previewName.setText(newVal.trim().toUpperCase());
                    }
                }
            });
        }

        // 2. Amount binding (fldAmount -> previewAmount & previewAmountWords)
        if (fldAmount != null) {
            fldAmount.textProperty().addListener((obs, oldVal, newVal) -> {
                if (previewAmount != null) {
                    if (newVal == null || newVal.isBlank()) {
                        previewAmount.setText("0.00");
                        if (previewAmountWords != null) {
                            previewAmountWords.setText("Zero Rupees Only");
                        }
                    } else {
                        try {
                            BigDecimal val = new BigDecimal(newVal.trim());
                            previewAmount.setText(String.format("%,.2f", val));
                            if (previewAmountWords != null) {
                                previewAmountWords.setText(com.chequeprint.util.NumberToWordsConverter.convert(val));
                            }
                        } catch (Exception ex) {
                            previewAmount.setText(newVal);
                        }
                    }
                }
            });
        }

        // 3. Bank binding (cmbBank -> previewBank)
        if (cmbBank != null) {
            cmbBank.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (previewBank != null) {
                    if (newVal == null || newVal.isBlank()) {
                        previewBank.setText("STATE BANK OF INDIA");
                    } else {
                        previewBank.setText(newVal.toUpperCase());
                    }
                }
            });
        }

        // 4. Date binding (datePicker -> previewDate)
        if (datePicker != null) {
            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (previewDate != null) {
                    if (newVal != null) {
                        previewDate.setText(newVal.format(java.time.format.DateTimeFormatter.ofPattern("dd / MM / yyyy")));
                    } else {
                        previewDate.setText("DD / MM / YYYY");
                    }
                }
            });
        }
    }

    public void initData(Cheque cheque) {
        this.selectedCheque = cheque;
        
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
            try {
                List<Bank> banks = bankService.getAll();
                Platform.runLater(() -> {
                    bankNameToId.clear();
                    ObservableList<String> names = FXCollections.observableArrayList();
                    for (Bank b : banks) {
                        names.add(b.getBankName());
                        bankNameToId.put(b.getBankName(), b.getId());
                    }
                    if (names.isEmpty()) {
                        names.addAll("SBI", "HDFC", "ICICI", "Axis Bank");
                    }
                    cmbBank.setItems(names);
                    
                    if (selectedCheque != null && selectedCheque.getBankName() != null && names.contains(selectedCheque.getBankName())) {
                        cmbBank.setValue(selectedCheque.getBankName());
                    } else if (!names.isEmpty()) {
                        cmbBank.setValue(names.get(0));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    cmbBank.setItems(FXCollections.observableArrayList("SBI", "HDFC", "ICICI", "Axis Bank"));
                    if (selectedCheque != null && selectedCheque.getBankName() != null) {
                        cmbBank.setValue(selectedCheque.getBankName());
                    } else {
                        cmbBank.setValue("SBI");
                    }
                });
            }
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
            int bankId = bankNameToId.getOrDefault(selectedBankName,
                    Math.max(1, cmbBank.getSelectionModel().getSelectedIndex() + 1));

            if (selectedCheque == null) {
                Cheque c = new Cheque(null, payee, amount, bankId, datePicker.getValue());
                workflowService.createPending(c, actor);
                showAlert("Success", "Cheque created and submitted for approval.", Alert.AlertType.INFORMATION);
            } else {
                selectedCheque.setPayeeName(payee);
                selectedCheque.setAmount(amount);
                selectedCheque.setBankId(bankId);
                selectedCheque.setIssueDate(datePicker.getValue());
                if (!chequeService.update(selectedCheque)) {
                    throw new RuntimeException("Could not update cheque.");
                }
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

            // Print the Cheque Preview Node using native JavaFX PrinterJob
            javafx.stage.Window window = btnSaveAndPrint.getScene() != null ? btnSaveAndPrint.getScene().getWindow() : null;
            boolean printed = FxPrinterService.printNode(chequePreviewCard, window);

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
