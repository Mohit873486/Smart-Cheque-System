package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MainController — manages the app shell (sidebar + content pane).
 *
 * FIXES APPLIED:
 *
 * FIX 1 — navigate() inlines the page-switch animation instead of delegating
 * to FxUtils.switchPage(). Previously, animating a node that was being
 * detached mid-animation caused setOnFinished() to silently not fire on some
 * JFX builds, so the new view was never added to contentPane. The fix:
 * contentPane.getChildren().setAll(view) now runs ONLY inside setOnFinished()
 * — i.e., after the fade-out is 100% complete and the old node is no longer
 * being animated, making detachment safe.
 *
 * FIX 2 — view.setOpacity(0) is set BEFORE adding the node to the scene,
 * eliminating the single-frame flash at full opacity.
 */
public class MainController {

    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentPane;
    @FXML
    private Label headerTitle;

    @FXML
    private HBox navDashboard;
    @FXML
    private HBox navCheques;
    @FXML
    private HBox navInvoices;
    @FXML
    private HBox navBanks;
    @FXML
    private HBox navProfile;
    @FXML
    private HBox navSettings;
    @FXML
    private HBox navSupport;

    private HBox activeNavItem;

    private final Map<String, String> fxmlMap = new HashMap<>() {
        {
            put("dashboard", "/view/dashboard.fxml");
            put("cheques", "/view/cheques.fxml");
            put("invoices", "/view/invoices.fxml");
            put("banks", "/view/banks.fxml");
            put("profile", "/view/profile.fxml");
            put("settings", "/view/settings.fxml");
            put("support", "/view/support.fxml");
        }
    };

    private final Map<String, String> titleMap = new HashMap<>() {
        {
            put("dashboard", "Dashboard");
            put("cheques", "Cheque Management");
            put("invoices", "Invoice Management");
            put("banks", "Bank Templates");
            put("profile", "My Profile");
            put("settings", "Settings");
            put("support", "Support Center");
        }
    };

    @FXML
    public void initialize() {
        int delay = 0;
        for (Node child : sidebar.getChildren()) {
            FxUtils.animateIn(child, delay);
            delay += 40;
        }
        navigate("dashboard");
        setActiveNav(navDashboard);
    }

    @FXML
    private void onDashboard() {
        navigate("dashboard");
        setActiveNav(navDashboard);
    }

    @FXML
    private void onCheques() {
        navigate("cheques");
        setActiveNav(navCheques);
    }

    @FXML
    private void onInvoices() {
        navigate("invoices");
        setActiveNav(navInvoices);
    }

    @FXML
    private void onBanks() {
        navigate("banks");
        setActiveNav(navBanks);
    }

    @FXML
    private void onProfile() {
        navigate("profile");
        setActiveNav(navProfile);
    }

    @FXML
    private void onSettings() {
        navigate("settings");
        setActiveNav(navSettings);
    }

    @FXML
    private void onSupport() {
        navigate("support");
        setActiveNav(navSupport);
    }

    public void navigate(String page) {
        String path = fxmlMap.get(page);
        if (path == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node view = loader.load();

            // FIX 2: invisible before entering the scene — no flash
            view.setOpacity(0);

            // Inject back-reference so sub-controllers can navigate
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController dc)
                dc.setMainController(this);
            if (ctrl instanceof ChequeController cc)
                cc.setMainController(this);
            if (ctrl instanceof InvoiceController ic)
                ic.setMainController(this);

            Node current = contentPane.getChildren().isEmpty()
                    ? null
                    : contentPane.getChildren().get(0);

            if (current != null) {
                // FIX 1: fade OUT first; only THEN swap + fade in
                FadeTransition fadeOut = new FadeTransition(Duration.millis(140), current);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    // current is fully faded — safe to detach it now
                    contentPane.getChildren().setAll(view);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(220), view);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                // First load — no outgoing node
                contentPane.getChildren().setAll(view);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), view);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }

            headerTitle.setText(titleMap.getOrDefault(page, "ChequePro"));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to navigate to " + page + ": " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (Exception e) {
            System.err.println("Unexpected error navigating to " + page + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void setActiveNav(HBox item) {
        if (activeNavItem != null)
            activeNavItem.getStyleClass().remove("nav-active");
        activeNavItem = item;
        if (item != null)
            item.getStyleClass().add("nav-active");
    }
}
