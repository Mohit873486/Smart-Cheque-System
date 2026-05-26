package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.service.ChequeService;
import com.chequeprint.service.InvoiceService;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.animation.*;
import javafx.util.Duration;

public class DashboardController {

    // =========================
    // OLD SUMMARY CARDS
    // =========================
    @FXML
    private Label lblTotalCheques;
    @FXML
    private Label lblPrinted;
    @FXML
    private Label lblPending;
    @FXML
    private Label lblMonthlyAmount;

    // =========================
    // NEW DASHBOARD METRICS
    // =========================
    @FXML
    private Label lblPendingCheques;
    @FXML
    private Label lblPrintedCheques;
    @FXML
    private Label lblTodayEntries;
    @FXML
    private Label lblTotalInvoices;
    @FXML
    private Label lblTotalAmount;

    @FXML
    private Label lblDraftCount;
    @FXML
    private Label lblPendingCount;
    @FXML
    private Label lblPrintedCount;
    @FXML
    private Label lblClearedCount;

    // =========================
    // PROGRESS BARS
    // =========================
    @FXML
    private ProgressBar pbDraft;
    @FXML
    private ProgressBar pbPending;
    @FXML
    private ProgressBar pbPrinted;
    @FXML
    private ProgressBar pbCleared;

    // =========================
    // RECENT TABLES
    // =========================
    @FXML
    private TableView<Cheque> tblRecentCheques;
    @FXML
    private TableColumn<Cheque, String> colChequeNo;
    @FXML
    private TableColumn<Cheque, String> colPayee;
    @FXML
    private TableColumn<Cheque, String> colAmount;
    @FXML
    private TableColumn<Cheque, String> colStatus;

    @FXML
    private TableView<Invoice> tblRecentInvoices;
    @FXML
    private TableColumn<Invoice, String> colInvoiceNo;
    @FXML
    private TableColumn<Invoice, String> colInvoiceClient;
    @FXML
    private TableColumn<Invoice, String> colInvoiceAmount;
    @FXML
    private TableColumn<Invoice, String> colInvoiceDue;
    @FXML
    private TableColumn<Invoice, String> colInvoiceStatus;

    // =========================
    // BUTTONS
    // =========================
    @FXML
    private Button btnNewCheque;
    @FXML
    private Button btnNewInvoice;
    @FXML
    private Button btnAddBank;
    @FXML
    private Button btnViewAllCheques;
    @FXML
    private Button btnViewAllInvoices;

    // =========================
    // CARDS (UI ANIMATION)
    // =========================
    @FXML
    private VBox cardTotal;
    @FXML
    private VBox cardPrinted;
    @FXML
    private VBox cardPending;
    @FXML
    private VBox cardAmount;

    // =========================
    // HEADER
    // =========================
    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblSubtitle;

    // =========================
    // ROOT LAYOUT
    // =========================
    @FXML
    private BorderPane dashboardRoot;
    @FXML
    private HBox statsRow;

    private final ChequeService service = new ChequeService();
    private final InvoiceService invoiceService = new InvoiceService();
    private MainController mainController;
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {

        System.out.println("DashboardController initialized");

        // =========================
        // SAFE NULL CHECK (DEBUG ONLY)
        // =========================
        safeCheck("lblWelcome", lblWelcome);
        safeCheck("lblSubtitle", lblSubtitle);
        safeCheck("cardTotal", cardTotal);

        // =========================
        // TABLE SETUP
        // =========================
        initializeRecentTables();

        // =========================
        // HEADER TEXT
        // =========================
        if (lblWelcome != null)
            lblWelcome.setText("Welcome Mohit ✅");
        if (lblSubtitle != null)
            lblSubtitle.setText("Finance Dashboard");

        FxUtils.animateIn(lblWelcome, 0);
        FxUtils.animateIn(lblSubtitle, 60);

        startAutoRefresh();

        // Load data asynchronously (also callable externally via `reload()`)
        reload();
    }

    /**
     * Reloads dashboard metrics and recent tables on a background thread.
     * Can be called by other controllers (e.g. after inserting a cheque).
     */
    public void reload() {
        new Thread(() -> {
            try {
                int total = service.getTotalCheques();
                int printed = service.getPrintedCheques();
                int pending = service.getPendingCheques();
                int today = service.getTodayCheques();
                double amount = service.getMonthlyAmount();
                int invoice = invoiceService.getTotalInvoices();
                var recentCheques = service.getAll();
                var recentInvoices = invoiceService.getAll();

                Platform.runLater(() -> {
                    // animations
                    animateCard(cardTotal, 80);
                    animateCard(cardPrinted, 160);
                    animateCard(cardPending, 240);
                    animateCard(cardAmount, 320);

                    // =========================
                    // OLD LABELS
                    // =========================
                    setCount(lblTotalCheques, total, "");
                    setCount(lblPrinted, printed, "");
                    setCount(lblPending, pending, "");
                    setCount(lblMonthlyAmount, (int) amount, "₹");

                    // =========================
                    // NEW LABELS
                    // =========================
                    setCount(lblTotalInvoices, invoice, "");
                    setCount(lblTotalAmount, (int) amount, "₹");
                    setCount(lblPendingCheques, pending, "");
                    setCount(lblPrintedCheques, printed, "");
                    setCount(lblTodayEntries, today, "");

                    // =========================
                    // RECENT TABLES
                    // =========================
                    if (tblRecentCheques != null && recentCheques != null) {
                        tblRecentCheques.getItems().setAll(recentCheques);
                    }
                    if (tblRecentInvoices != null && recentInvoices != null) {
                        tblRecentInvoices.getItems().setAll(recentInvoices);
                    }

                    // =========================
                    // CHEQUE STATUS SUMMARY
                    // Use recent rows when available, otherwise fall back to global service counts
                    // =========================
                    int totalForProgress = 1; // avoid division by zero
                    long draft = 0, pendingCountLong = 0, printedCountLong = 0, cancelled = 0;

                    if (recentCheques != null && !recentCheques.isEmpty()) {
                        totalForProgress = recentCheques.size();
                        draft = recentCheques.stream().filter(c -> c.getStatus() == Cheque.Status.Draft).count();
                        pendingCountLong = recentCheques.stream().filter(c -> c.getStatus() == Cheque.Status.Pending)
                                .count();
                        printedCountLong = recentCheques.stream().filter(c -> c.getStatus() == Cheque.Status.Printed)
                                .count();
                        cancelled = recentCheques.stream().filter(c -> c.getStatus() == Cheque.Status.Cancelled)
                                .count();
                    } else {
                        totalForProgress = Math.max(total, 1);
                        printedCountLong = printed;
                        pendingCountLong = pending;
                        long est = totalForProgress - printedCountLong - pendingCountLong;
                        draft = Math.max(est, 0);
                        cancelled = 0;
                    }

                    if (lblDraftCount != null)
                        lblDraftCount.setText(String.valueOf(draft));
                    if (lblPendingCount != null)
                        lblPendingCount.setText(String.valueOf(pendingCountLong));
                    if (lblPrintedCount != null)
                        lblPrintedCount.setText(String.valueOf(printedCountLong));
                    if (lblClearedCount != null)
                        lblClearedCount.setText(String.valueOf(cancelled));

                    if (pbDraft != null) {
                        pbDraft.setProgress((double) draft / totalForProgress);
                        replaceProgressBarWithSegmentBar(pbDraft, totalForProgress, (int) draft, "#475569");
                    }
                    if (pbPending != null) {
                        pbPending.setProgress((double) pendingCountLong / totalForProgress);
                        replaceProgressBarWithSegmentBar(pbPending, totalForProgress, (int) pendingCountLong,
                                "#92400e");
                    }
                    if (pbPrinted != null) {
                        pbPrinted.setProgress((double) printedCountLong / totalForProgress);
                        replaceProgressBarWithSegmentBar(pbPrinted, totalForProgress, (int) printedCountLong,
                                "#065f46");
                    }
                    if (pbCleared != null) {
                        pbCleared.setProgress((double) cancelled / totalForProgress);
                        replaceProgressBarWithSegmentBar(pbCleared, totalForProgress, (int) cancelled, "#991b1b");
                    }

                    addHover(cardTotal);
                    addHover(cardPrinted);
                    addHover(cardPending);
                    addHover(cardAmount);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (lblWelcome != null)
                        lblWelcome.setText("⚠ Database Error");
                });
            }
        }).start();
    }

    // =========================
    // HELPER METHODS
    // =========================

    private void setCount(Label label, int value, String prefix) {
        if (label != null) {
            FxUtils.countUp(label, value, prefix, 100);
        }
    }

    private void animateCard(VBox card, int delay) {
        if (card != null) {
            FxUtils.bounceIn(card, delay);
        }
    }

    @FXML
    private void onNewCheque() {
        if (mainController != null) {
            mainController.showCheques();
        }
    }

    @FXML
    private void onNewInvoice() {
        if (mainController != null) {
            mainController.showInvoices();
        }
    }

    @FXML
    private void onAddBank() {
        if (mainController != null) {
            mainController.showBanks();
        }
    }

    @FXML
    private void onViewAllCheques() {
        if (mainController != null) {
            mainController.showCheques();
        }
    }

    @FXML
    private void onViewAllInvoices() {
        if (mainController != null) {
            mainController.showInvoices();
        }
    }

    private void addHover(VBox card) {
        if (card != null) {
            FxUtils.addHoverEffect(card);
        }
    }

    private void safeCheck(String name, Object obj) {
        if (obj == null) {
            System.out.println("FXML WARNING: " + name + " is NULL");
        }
    }

    private void initializeRecentTables() {
        if (tblRecentCheques != null) {
            colChequeNo.setCellValueFactory(new PropertyValueFactory<>("chequeNo"));
            colPayee.setCellValueFactory(new PropertyValueFactory<>("payeeName"));
            colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getAmount() != null ? "₹" + c.getValue().getAmount().toPlainString() : ""));
            colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getStatus() != null ? c.getValue().getStatus().name() : "Unknown"));
            // Color-code status similar to ChequeController
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
                    setStyle(switch (item) {
                        case "Printed" -> "-fx-text-fill:#065f46;-fx-font-weight:bold;";
                        case "Pending" -> "-fx-text-fill:#92400e;-fx-font-weight:bold;";
                        case "Draft" -> "-fx-text-fill:#475569;-fx-font-weight:bold;";
                        case "Cancelled" -> "-fx-text-fill:#991b1b;-fx-font-weight:bold;";
                        default -> "";
                    });
                }
            });
        }

        if (tblRecentInvoices != null) {
            colInvoiceNo.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
            colInvoiceClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
            colInvoiceAmount.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getAmount() != null ? "₹" + c.getValue().getAmount().toPlainString() : ""));
            colInvoiceDue.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getDueDate() != null ? c.getValue().getDueDate().toString() : ""));
            colInvoiceStatus.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getStatus() != null ? c.getValue().getStatus().name() : "Unpaid"));
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> reload()));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    // =========================
    // MAIN CONTROLLER LINK
    // =========================
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // Replace a ProgressBar with a segmented HBox showing discrete items.
    // If replacement isn't possible (parent not a Pane), falls back to styling the
    // ProgressBar accent.
    private void replaceProgressBarWithSegmentBar(ProgressBar pb, int totalItems, int filledItems, String colorHex) {
        if (pb == null)
            return;
        // cap segments to avoid extremely large counts
        int maxSegments = 12;
        int segments = Math.max(1, Math.min(totalItems, maxSegments));

        int filledScaled = 0;
        if (totalItems > 0) {
            filledScaled = (int) Math.round((double) filledItems / (double) Math.max(totalItems, 1) * segments);
        }

        final int segmentsFinal = segments;
        final int filledScaledFinal = filledScaled;

        Platform.runLater(() -> {
            try {
                Parent parent = pb.getParent();
                if (!(parent instanceof Pane)) {
                    // fallback: color the bar
                    pb.setStyle("-fx-accent: " + colorHex + ";");
                    return;
                }

                Pane pane = (Pane) parent;
                int idx = pane.getChildren().indexOf(pb);
                if (idx < 0) {
                    pb.setStyle("-fx-accent: " + colorHex + ";");
                    return;
                }

                HBox segBox = new HBox();
                segBox.getStyleClass().add("segmented-bar");
                segBox.setSpacing(6);
                segBox.setMaxWidth(Double.MAX_VALUE);

                for (int i = 0; i < segments; i++) {
                    Region seg = new Region();
                    seg.getStyleClass().add("segment");
                    seg.setPrefHeight(8);
                    HBox.setHgrow(seg, Priority.ALWAYS);
                    // set initial empty style
                    seg.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius:6;");
                    segBox.getChildren().add(seg);
                }

                // insert segmented box in place of ProgressBar
                pane.getChildren().set(idx, segBox);

                // animate fill for filledScaled segments
                for (int i = 0; i < filledScaledFinal; i++) {
                    final int segmentIndex = i;
                    Node node = segBox.getChildren().get(segmentIndex);
                    Timeline tl = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0)),
                            new KeyFrame(Duration.millis(350 + segmentIndex * 80),
                                    new KeyValue(node.opacityProperty(), 1)));
                    tl.play();
                    node.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius:6;");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                pb.setStyle("-fx-accent: " + colorHex + ";");
            }
        });
    }
}

