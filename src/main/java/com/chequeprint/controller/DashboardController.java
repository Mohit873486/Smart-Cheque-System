package com.chequeprint.controller;

import com.chequeprint.service.ChequeService;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

/**
 * DashboardController — loads live stats from DB, animates stat cards.
 *
 * Fixes applied vs original:
 * • Removed @FXML nameField, amountField, table — these don't exist in
 * dashboard.fxml and caused FXMLLoader to throw on injection.
 * • Removed @FXML minimizeWindow() — it referenced a Node that dashboard.fxml
 * doesn't own; the method is only valid from the top-level stage owner.
 * • Removed dead addCheque() stub.
 * • recentSection null-guard added (it's optional in the FXML).
 */
public class DashboardController {

    // Stat card labels
    @FXML
    private Label lblTotalCheques;
    @FXML
    private Label lblPrinted;
    @FXML
    private Label lblPending;
    @FXML
    private Label lblMonthlyAmount;

    // Card containers for animation
    @FXML
    private VBox cardTotal;
    @FXML
    private VBox cardPrinted;
    @FXML
    private VBox cardPending;
    @FXML
    private VBox cardAmount;

    // Welcome header
    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblSubtitle;

    // Layout
    @FXML
    private VBox dashboardRoot;
    @FXML
    private HBox statsRow;
    @FXML
    private VBox recentSection;

    // Injected by MainController
    private MainController mainController;

    private final ChequeService service = new ChequeService();

    // ── Initialise ───────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            System.out.println("DashboardController.initialize() called");
            System.out.println("lblWelcome: " + lblWelcome);
            System.out.println("lblSubtitle: " + lblSubtitle);
            System.out.println("cardTotal: " + cardTotal);
            System.out.println("statsRow: " + statsRow);

            if (lblWelcome == null) {
                System.err.println("ERROR: lblWelcome is null!");
                return;
            }
            if (lblSubtitle == null) {
                System.err.println("ERROR: lblSubtitle is null!");
                return;
            }
            if (cardTotal == null) {
                System.err.println("ERROR: cardTotal is null!");
                return;
            }

            FxUtils.animateIn(lblWelcome, 0);
            FxUtils.animateIn(lblSubtitle, 60);

            // Load DB stats in background to keep UI thread free
            new Thread(() -> {
                try {
                    System.out.println("Loading dashboard stats from database...");
                    int total = service.getTotalCheques();
                    int printed = service.getPrintedCheques();
                    int pending = service.getPendingCheques();
                    double amt = service.getMonthlyAmount();
                    System.out.println(
                            "Dashboard stats loaded: total=" + total + ", printed=" + printed + ", pending=" + pending);

                    Platform.runLater(() -> {
                        // Staggered bounce-in animations
                        FxUtils.bounceIn(cardTotal, 80);
                        FxUtils.bounceIn(cardPrinted, 160);
                        FxUtils.bounceIn(cardPending, 240);
                        FxUtils.bounceIn(cardAmount, 320);

                        // Animated count-up
                        FxUtils.countUp(lblTotalCheques, total, "", 80);
                        FxUtils.countUp(lblPrinted, printed, "", 160);
                        FxUtils.countUp(lblPending, pending, "", 240);
                        FxUtils.countUp(lblMonthlyAmount, (int) amt, "₹", 320);

                        // Hover pulse effects
                        FxUtils.addHoverEffect(cardTotal);
                        FxUtils.addHoverEffect(cardPrinted);
                        FxUtils.addHoverEffect(cardPending);
                        FxUtils.addHoverEffect(cardAmount);
                    });
                } catch (Exception e) {
                    System.err.println("Error loading dashboard stats: " + e.getMessage());
                    e.printStackTrace(System.err);
                    Platform.runLater(() -> {
                        if (lblWelcome != null) {
                            lblWelcome.setText("⚠ DB Error: " + e.getMessage());
                        }
                    });
                }
            }, "db-stats-loader").start();

            // Animate recent section (null-safe — field is optional)
            if (recentSection != null)
                FxUtils.animateIn(recentSection, 400);
        } catch (Exception e) {
            System.err.println("Error in DashboardController.initialize(): " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    // ── Navigation handlers ──────────────────────────────────────────
    @FXML
    private void onViewAllCheques() {
        if (mainController != null)
            mainController.navigate("cheques");
    }

    @FXML
    private void onViewAllInvoices() {
        if (mainController != null)
            mainController.navigate("invoices");
    }

    public void setMainController(MainController mc) {
        this.mainController = mc;
    }
}