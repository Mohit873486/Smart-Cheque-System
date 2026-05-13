package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.service.ChequeService;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DashboardController {

    // =========================
    // OLD SUMMARY CARDS
    // =========================
    @FXML private Label lblTotalCheques;
    @FXML private Label lblPrinted;
    @FXML private Label lblPending;
    @FXML private Label lblMonthlyAmount;

    // =========================
    // NEW DASHBOARD METRICS
    // =========================
    @FXML private Label lblPendingCheques;
    @FXML private Label lblTotalInvoices;
    @FXML private Label lblTotalAmount;

    @FXML private Label lblDraftCount;
    @FXML private Label lblPendingCount;
    @FXML private Label lblPrintedCount;
    @FXML private Label lblClearedCount;

    // =========================
    // PROGRESS BARS
    // =========================
    @FXML private ProgressBar pbDraft;
    @FXML private ProgressBar pbPending;
    @FXML private ProgressBar pbPrinted;
    @FXML private ProgressBar pbCleared;

    // =========================
    // RECENT TABLES
    // =========================
    @FXML private TableView<Cheque> tblRecentCheques;
    @FXML private TableColumn<Cheque, String> colChequeNo;
    @FXML private TableColumn<Cheque, String> colPayee;
    @FXML private TableColumn<Cheque, String> colAmount;
    @FXML private TableColumn<Cheque, String> colStatus;

    @FXML private TableView<Invoice> tblRecentInvoices;
    @FXML private TableColumn<Invoice, String> colInvoiceNo;
    @FXML private TableColumn<Invoice, String> colInvoiceClient;
    @FXML private TableColumn<Invoice, String> colInvoiceAmount;
    @FXML private TableColumn<Invoice, String> colInvoiceDue;
    @FXML private TableColumn<Invoice, String> colInvoiceStatus;

    // =========================
    // BUTTONS
    // =========================
    @FXML private Button btnNewCheque;
    @FXML private Button btnNewInvoice;
    @FXML private Button btnAddBank;
    @FXML private Button btnViewAllCheques;
    @FXML private Button btnViewAllInvoices;

    // =========================
    // CARDS (UI ANIMATION)
    // =========================
    @FXML private VBox cardTotal;
    @FXML private VBox cardPrinted;
    @FXML private VBox cardPending;
    @FXML private VBox cardAmount;

    // =========================
    // HEADER
    // =========================
    @FXML private Label lblWelcome;
    @FXML private Label lblSubtitle;

    // =========================
    // ROOT LAYOUT
    // =========================
    @FXML private VBox dashboardRoot;
    @FXML private HBox statsRow;

    private final ChequeService service = new ChequeService();
    private MainController mainController;

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
        // HEADER TEXT
        // =========================
        if (lblWelcome != null) lblWelcome.setText("Welcome Mohit ✅");
        if (lblSubtitle != null) lblSubtitle.setText("Finance Dashboard");

        FxUtils.animateIn(lblWelcome, 0);
        FxUtils.animateIn(lblSubtitle, 60);

        // =========================
        // LOAD DATA BACKGROUND THREAD
        // =========================
        new Thread(() -> {
            try {
                int total = service.getTotalCheques();
                int printed = service.getPrintedCheques();
                int pending = service.getPendingCheques();
                double amount = service.getMonthlyAmount();

                // int invoices = service.getTotalInvoices(); // TODO: Add getTotalInvoices() to ChequeService
                int invoices = 0;

                Platform.runLater(() -> {

                    // =========================
                    // ANIMATIONS
                    // =========================
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
                    setCount(lblTotalInvoices, invoices, "");
                    setCount(lblTotalAmount, (int) amount, "₹");
                    setCount(lblPendingCheques, pending, "");

                    // =========================
                    // HOVER EFFECTS
                    // =========================
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

    // =========================
    // MAIN CONTROLLER LINK
    // =========================
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}