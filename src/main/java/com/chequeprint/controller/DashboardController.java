package com.chequeprint.controller;

import com.chequeprint.service.ChequeService;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DashboardController {

    // Labels
    @FXML private Label lblTotalCheques;
    @FXML private Label lblPrinted;
    @FXML private Label lblPending;
    @FXML private Label lblMonthlyAmount;

    // Cards
    @FXML private VBox cardTotal;
    @FXML private VBox cardPrinted;
    @FXML private VBox cardPending;
    @FXML private VBox cardAmount;

    // Header
    @FXML private Label lblWelcome;
    @FXML private Label lblSubtitle;

    // Layout
    @FXML private VBox dashboardRoot;
    @FXML private HBox statsRow;

    private final ChequeService service = new ChequeService();

    @FXML
    public void initialize() {

        System.out.println("DashboardController.initialize() called");

        // ✅ SAFE CHECKS
        if (lblWelcome == null) System.out.println("ERROR: lblWelcome null");
        if (lblSubtitle == null) System.out.println("ERROR: lblSubtitle null");
        if (cardTotal == null) System.out.println("ERROR: cardTotal null");
        if (cardPrinted == null) System.out.println("ERROR: cardPrinted null");

        // ✅ TEXT SET
        lblWelcome.setText("Welcome Mohit ✅");
        lblSubtitle.setText("Finance Dashboard");

        // Animations
        FxUtils.animateIn(lblWelcome, 0);
        FxUtils.animateIn(lblSubtitle, 60);

        // Load DB data
        new Thread(() -> {
            try {
                int total = service.getTotalCheques();
                int printed = service.getPrintedCheques();
                int pending = service.getPendingCheques();
                double amt = service.getMonthlyAmount();

                Platform.runLater(() -> {

                    FxUtils.bounceIn(cardTotal, 80);
                    FxUtils.bounceIn(cardPrinted, 160);
                    FxUtils.bounceIn(cardPending, 240);
                    FxUtils.bounceIn(cardAmount, 320);

                    FxUtils.countUp(lblTotalCheques, total, "", 80);
                    FxUtils.countUp(lblPrinted, printed, "", 160);
                    FxUtils.countUp(lblPending, pending, "", 240);
                    FxUtils.countUp(lblMonthlyAmount, (int) amt, "₹", 320);

                    FxUtils.addHoverEffect(cardTotal);
                    FxUtils.addHoverEffect(cardPrinted);
                    FxUtils.addHoverEffect(cardPending);
                    FxUtils.addHoverEffect(cardAmount);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        lblWelcome.setText("⚠ DB Error")
                );
            }
        }).start();
    }

    private MainController mainController;
    public void setMainController(MainController mainController) {
    this.mainController = mainController;
}
}