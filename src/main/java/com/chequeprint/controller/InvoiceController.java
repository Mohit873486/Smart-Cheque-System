package com.chequeprint.controller;

import com.chequeprint.model.Invoice;
import com.chequeprint.service.InvoiceService;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.JasperPrintUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class InvoiceController {

    // ── Table columns ────────────────────────────────────────────────
    @FXML
    private TableView<Invoice> invoiceTable;
    @FXML
    private TableColumn<Invoice, String> colInvoiceNo;
    @FXML
    private TableColumn<Invoice, String> colClient;
    @FXML
    private TableColumn<Invoice, String> colAmount;
    @FXML
    private TableColumn<Invoice, String> colIssueDate;
    @FXML
    private TableColumn<Invoice, String> colDueDate;
    @FXML
    private TableColumn<Invoice, String> colStatus;

    // ── Form fields ──────────────────────────────────────────────────
    @FXML
    private TextField fldClient;
    @FXML
    private TextField fldAmount;
    @FXML
    private DatePicker dateIssue;
    @FXML
    private DatePicker dateDue;
    @FXML
    private TextArea fldNotes;
    @FXML
    private Label lblFormTitle;
    @FXML
    private TextField searchField;
    @FXML
    private VBox rootPane;

    // ── Injected by MainController ───────────────────────────────────
    private MainController mainController;

    // ── State ────────────────────────────────────────────────────────
    private final InvoiceService service = new InvoiceService();
    private final ObservableList<Invoice> data = FXCollections.observableArrayList();
    private Invoice selectedInvoice;

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupTable();
        loadData();
        setupSearchFilter();
        dateIssue.setValue(LocalDate.now());
        dateDue.setValue(LocalDate.now().plusDays(30));
        FxUtils.animateIn(rootPane, 0);
    }

    // ── Table setup ──────────────────────────────────────────────────
    private void setupTable() {
        colInvoiceNo.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));

        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAmount() != null
                        ? "₹" + String.format("%,.2f", c.getValue().getAmount())
                        : "₹0.00"));

        colIssueDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIssueDate() != null
                        ? c.getValue().getIssueDate().toString()
                        : ""));

        colDueDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDueDate() != null
                        ? c.getValue().getDueDate().toString()
                        : ""));

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null
                        ? c.getValue().getStatus().name()
                        : ""));

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                switch (item) {
                    case "Paid" -> setStyle("-fx-text-fill:#065f46;-fx-font-weight:bold;");
                    case "Unpaid" -> setStyle("-fx-text-fill:#991b1b;-fx-font-weight:bold;");
                    case "Partial" -> setStyle("-fx-text-fill:#1e40af;-fx-font-weight:bold;");
                    case "Cancelled" -> setStyle("-fx-text-fill:#475569;");
                    default -> setStyle("");
                }
            }
        });

        invoiceTable.setItems(data);

        invoiceTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null)
                        populateForm(sel);
                });
    }

    // ── Load data ────────────────────────────────────────────────────
    private void loadData() {
        new Thread(() -> {
            try {
                var list = service.getAll();
                Platform.runLater(() -> data.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "load-invoices").start();
    }

    // Serch Filter
    private void setupSearchFilter() {

        FilteredList<Invoice> filteredData = new FilteredList<>(data, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(invoice -> {

                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }

                String keyword = newVal.toLowerCase();

                // search logic (client + invoice no)
                if (invoice.getClientName() != null &&
                        invoice.getClientName().toLowerCase().contains(keyword)) {
                    return true;
                }

                if (invoice.getInvoiceNo() != null &&
                        invoice.getInvoiceNo().toLowerCase().contains(keyword)) {
                    return true;
                }

                return false;
            });
        });

        SortedList<Invoice> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(invoiceTable.comparatorProperty());

        invoiceTable.setItems(sortedData);
    }

    // ── Save / Update ────────────────────────────────────────────────
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
            clearForm();
            loadData();
            // Notify dashboard (if loaded) to reload its metrics and recent tables
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController)
                    ((DashboardController) dc).reload();
            }
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Save and Direct Print ────────────────────────────────────────
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

            loadData();

            boolean printed = openPrintPreview(invoiceToPrint);
            if (!printed) {
                showAlert("Print Canceled", "Invoice printing was canceled.",
                        Alert.AlertType.INFORMATION);
                return;
            }

            showAlert("Success", "Invoice printed successfully.",
                    Alert.AlertType.INFORMATION);

            clearForm();
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController)
                    ((DashboardController) dc).reload();
            }
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── ───────────────────────────────────────────────────
    @FXML
    private void onExportPdf() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to export.",
                    Alert.AlertType.WARNING);
            return;
        }
        try {
            String desktopPath = System.getProperty("user.home")
                    + java.io.File.separator + "Desktop";

            String savedPath = JasperPrintUtil.exportInvoicePdf(sel, desktopPath);

            showAlert("PDF Exported",
                    "Invoice saved to:\n" + savedPath,
                    Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Export Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onPrint() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to print.",
                    Alert.AlertType.WARNING);
            return;
        }
        try {
            boolean printed = openPrintPreview(sel);
            if (!printed) {
                showAlert("Print Canceled", "Invoice printing was canceled.",
                        Alert.AlertType.INFORMATION);
                return;
            }
            loadData();
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController) {
                    ((DashboardController) dc).reload();
                }
            }
            showAlert("Print Successful",
                    "Invoice printed successfully.",
                    Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Print Error", e.getMessage(), Alert.AlertType.ERROR);
        }
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

    // ── Delete ───────────────────────────────────────────────────────
    @FXML
    private void onDelete() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to delete.",
                    Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete invoice " + sel.getInvoiceNo() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.delete(sel.getId());
                    clearForm();
                    loadData();
                    if (mainController != null) {
                        Object dc = mainController.getController("dashboard");
                        if (dc instanceof DashboardController)
                            ((DashboardController) dc).reload();
                    }

                } catch (Exception e) {
                    showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    // ── Form helpers ─────────────────────────────────────────────────
    private void populateForm(Invoice inv) {
        selectedInvoice = inv;
        lblFormTitle.setText("Edit Invoice");
        fldClient.setText(inv.getClientName() != null ? inv.getClientName() : "");
        fldAmount.setText(inv.getAmount() != null ? inv.getAmount().toPlainString() : "");
        dateIssue.setValue(inv.getIssueDate() != null ? inv.getIssueDate() : LocalDate.now());
        dateDue.setValue(inv.getDueDate() != null ? inv.getDueDate() : LocalDate.now().plusDays(30));
        fldNotes.setText(inv.getNotes() != null ? inv.getNotes() : "");
        FxUtils.animateIn(rootPane, 0);
    }

    private void clearForm() {
        selectedInvoice = null;
        lblFormTitle.setText("New Invoice");
        fldClient.clear();
        fldAmount.clear();
        fldNotes.clear();
        dateIssue.setValue(LocalDate.now());
        dateDue.setValue(LocalDate.now().plusDays(30));
        invoiceTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public void setMainController(MainController mc) {
        this.mainController = mc;
    }

}
