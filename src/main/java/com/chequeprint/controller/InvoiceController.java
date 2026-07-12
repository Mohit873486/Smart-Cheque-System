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

    // Form fields moved to InvoiceDialogController
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
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("status-badge");
                switch (item) {
                    case "Paid" -> badge.getStyleClass().add("status-printed");
                    case "Unpaid" -> badge.getStyleClass().add("status-cancelled");
                    case "Partial" -> badge.getStyleClass().add("status-pending");
                    default -> badge.getStyleClass().add("status-neutral");
                }
                setGraphic(badge);
                setText(null);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        invoiceTable.setItems(data);

        invoiceTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Invoice> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Invoice rowData = row.getItem();
                    openInvoiceDialog(rowData);
                }
            });
            return row;
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

    public void applyMainSearch(String query) {
        if (searchField != null) {
            searchField.setText(query == null ? "" : query.trim());
        }
    }

    @FXML
    private void onNewInvoice() {
        openInvoiceDialog(null);
    }

    @FXML
    private void onEditInvoice() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to edit.", Alert.AlertType.WARNING);
            return;
        }
        openInvoiceDialog(sel);
    }

    private void openInvoiceDialog(Invoice invoice) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/invoice_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            InvoiceDialogController controller = loader.getController();
            controller.initData(invoice);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());
            stage.setTitle(invoice == null ? "New Invoice" : "Edit Invoice");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            com.chequeprint.util.ThemeManager.applySavedTheme(scene);

            // Drag support
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

            stage.setScene(scene);

            // Background blur/dim
            javafx.scene.Parent ownerRoot = rootPane.getScene().getRoot();
            javafx.scene.effect.Effect oldEffect = ownerRoot.getEffect();

            javafx.scene.effect.BoxBlur blur = new javafx.scene.effect.BoxBlur(6, 6, 3);
            javafx.scene.effect.ColorAdjust dim = new javafx.scene.effect.ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);

            ownerRoot.setEffect(dim);

            try {
                stage.showAndWait();
            } finally {
                ownerRoot.setEffect(oldEffect);
            }

            if (controller.isSaved()) {
                loadData();
                if (mainController != null) {
                    Object dc = mainController.getController("dashboard");
                    if (dc instanceof DashboardController) {
                        ((DashboardController) dc).reload();
                    }
                }
            }
        } catch (Exception e) {
            showAlert("Error", "Could not open invoice window: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void onExportPdf() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to export.", Alert.AlertType.WARNING);
            return;
        }
        try {
            String desktopPath = System.getProperty("user.home") + java.io.File.separator + "Desktop";
            String savedPath = JasperPrintUtil.exportInvoicePdf(sel, desktopPath);
            showAlert("PDF Exported", "Invoice saved to:\n" + savedPath, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Export Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onPrint() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to print.", Alert.AlertType.WARNING);
            return;
        }
        try {
            boolean printed = JasperPrintUtil.previewInvoice(sel);
            if (!printed) {
                showAlert("Print Canceled", "Invoice printing was canceled.", Alert.AlertType.INFORMATION);
                return;
            }
            loadData();
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController) {
                    ((DashboardController) dc).reload();
                }
            }
            showAlert("Print Successful", "Invoice printed successfully.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Print Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onDelete() {
        Invoice sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select an invoice to delete.", Alert.AlertType.WARNING);
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
                    invoiceTable.getSelectionModel().clearSelection();
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
