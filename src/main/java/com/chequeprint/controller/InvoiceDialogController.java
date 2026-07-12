package com.chequeprint.controller;

import com.chequeprint.model.Invoice;
import com.chequeprint.service.InvoiceService;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.JasperPrintUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * InvoiceDialogController manages popup dialog interactions for creating and updating invoices.
 */
public class InvoiceDialogController {

    @FXML
    private Label lblFormTitle;
    @FXML
    private TextField fldClient;
    @FXML
    private TextField fldAmount;
    @FXML
    private HBox moneyInputShell;
    @FXML
    private DatePicker dateIssue;
    @FXML
    private DatePicker dateDue;
    @FXML
    private TextArea fldNotes;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnSaveAndPrint;

    private final InvoiceService service = new InvoiceService();
    private Invoice selectedInvoice;
    private boolean saved = false;

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
    }

    public void initData(Invoice invoice) {
        this.selectedInvoice = invoice;
        if (invoice != null) {
            lblFormTitle.setText("Edit Invoice");
            fldClient.setText(invoice.getClientName() != null ? invoice.getClientName() : "");
            fldAmount.setText(invoice.getAmount() != null ? invoice.getAmount().toPlainString() : "");
            dateIssue.setValue(invoice.getIssueDate() != null ? invoice.getIssueDate() : LocalDate.now());
            dateDue.setValue(invoice.getDueDate() != null ? invoice.getDueDate() : LocalDate.now().plusDays(30));
            fldNotes.setText(invoice.getNotes() != null ? invoice.getNotes() : "");
        } else {
            lblFormTitle.setText("New Invoice");
            fldClient.clear();
            fldAmount.clear();
            fldNotes.clear();
            dateIssue.setValue(LocalDate.now());
            dateDue.setValue(LocalDate.now().plusDays(30));
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        String client = fldClient.getText().trim();
        String amtStr = fldAmount.getText().trim();

        if (client.isEmpty() || amtStr.isEmpty()) {
            FxUtils.shake(fldClient);
            showAlert("Validation", "Client name and amount are required.",
                    Alert.AlertType.WARNING);
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            FxUtils.shake(fldAmount);
            showAlert("Validation", "Enter a valid numeric amount (e.g. 5000.00).",
                    Alert.AlertType.WARNING);
            return;
        }

        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            FxUtils.shake(fldAmount);
            showAlert("Validation", "Amount must be greater than zero.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            if (selectedInvoice == null) {
                Invoice inv = new Invoice(null, client, amt,
                        dateIssue.getValue(), dateDue.getValue());
                inv.setNotes(fldNotes.getText());
                service.save(inv);
                showAlert("Success", "Invoice saved successfully.",
                        Alert.AlertType.INFORMATION);
            } else {
                selectedInvoice.setClientName(client);
                selectedInvoice.setAmount(amt);
                selectedInvoice.setIssueDate(dateIssue.getValue());
                selectedInvoice.setDueDate(dateDue.getValue());
                selectedInvoice.setNotes(fldNotes.getText());
                service.update(selectedInvoice);
                showAlert("Success", "Invoice updated.", Alert.AlertType.INFORMATION);
            }
            saved = true;
            closeStage();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onSaveAndPrint() {
        String client = fldClient.getText().trim();
        String amtStr = fldAmount.getText().trim();

        if (client.isEmpty() || amtStr.isEmpty()) {
            FxUtils.shake(fldClient);
            showAlert("Validation", "Client name and amount are required.",
                    Alert.AlertType.WARNING);
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            FxUtils.shake(fldAmount);
            showAlert("Validation", "Enter a valid numeric amount (e.g. 5000.00).",
                    Alert.AlertType.WARNING);
            return;
        }

        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            FxUtils.shake(fldAmount);
            showAlert("Validation", "Amount must be greater than zero.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            Invoice invoiceToPrint;
            if (selectedInvoice == null) {
                Invoice inv = new Invoice(null, client, amt,
                        dateIssue.getValue(), dateDue.getValue());
                inv.setNotes(fldNotes.getText());
                service.save(inv);
                invoiceToPrint = resolveLatestSavedInvoice(inv);
            } else {
                selectedInvoice.setClientName(client);
                selectedInvoice.setAmount(amt);
                selectedInvoice.setIssueDate(dateIssue.getValue());
                selectedInvoice.setDueDate(dateDue.getValue());
                selectedInvoice.setNotes(fldNotes.getText());
                service.update(selectedInvoice);
                invoiceToPrint = selectedInvoice;
            }

            boolean printed = openPrintPreview(invoiceToPrint);
            if (!printed) {
                showAlert("Print Canceled", "Invoice printing was canceled.",
                        Alert.AlertType.INFORMATION);
                return;
            }

            showAlert("Success", "Invoice printed successfully.",
                    Alert.AlertType.INFORMATION);

            saved = true;
            closeStage();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onClear() {
        if (fldClient != null) fldClient.clear();
        if (fldAmount != null) fldAmount.clear();
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) lblFormTitle.getScene().getWindow();
        stage.close();
    }

    private boolean openPrintPreview(Invoice invoice) throws Exception {
        return JasperPrintUtil.previewInvoice(invoice);
    }

    private Invoice resolveLatestSavedInvoice(Invoice created) throws Exception {
        if (created == null || created.getInvoiceNo() == null || created.getInvoiceNo().isBlank()) {
            return created;
        }
        return service.getAll().stream()
                .filter(i -> created.getInvoiceNo().equals(i.getInvoiceNo()))
                .max(Comparator.comparingInt(Invoice::getId))
                .orElse(created);
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
