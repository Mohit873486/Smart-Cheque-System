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
 * MainController — manages the app shell:
 *   - Left sidebar navigation
 *   - Top header (search, user avatar)
 *   - Center content pane (swaps FXML views)
 *
 * FIX 1: navigate() now inlines the fade-out/fade-in animation directly,
 *         instead of passing a Runnable to FxUtils.switchPage().
 *         Previously, animating a node that gets detached mid-animation
 *         caused setOnFinished() to silently never fire on some JFX builds,
 *         so the new view was never added to contentPane.
 */
public class MainController {

    @FXML private VBox      sidebar;
    @FXML private StackPane contentPane;
    @FXML private Label     headerTitle;

    // Sidebar nav buttons (fx:id must match main.fxml)
    @FXML private HBox navDashboard;
    @FXML private HBox navCheques;
    @FXML private HBox navInvoices;
    @FXML private HBox navBanks;
    @FXML private HBox navProfile;
    @FXML private HBox navSettings;
    @FXML private HBox navSupport;

    private HBox activeNavItem;

    private final Map<String, String> fxmlMap = new HashMap<>() {{
        put("dashboard", "/view/dashboard.fxml");
        put("cheques",   "/view/cheques.fxml");
        put("invoices",  "/view/invoices.fxml");
        put("banks",     "/view/banks.fxml");
        put("profile",   "/view/profile.fxml");
        put("settings",  "/view/settings.fxml");
        put("support",   "/view/support.fxml");
    }};

    private final Map<String, String> titleMap = new HashMap<>() {{
        put("dashboard", "Dashboard");
        put("cheques",   "Cheque Management");
        put("invoices",  "Invoice Management");
        put("banks",     "Bank Templates");
        put("profile",   "My Profile");
        put("settings",  "Settings");
        put("support",   "Support Center");
    }};

    @FXML
    public void initialize() {
        // Animate sidebar items in sequence
        int delay = 0;
        for (Node child : sidebar.getChildren()) {
            FxUtils.animateIn(child, delay);
            delay += 40;
        }
        // Default page on startup
        navigate("dashboard");
        setActiveNav(navDashboard);
    }

    // ── Nav button handlers (wired via onMouseClicked in main.fxml) ──

    @FXML private void onDashboard() { navigate("dashboard"); setActiveNav(navDashboard); }
    @FXML private void onCheques()   { navigate("cheques");   setActiveNav(navCheques);   }
    @FXML private void onInvoices()  { navigate("invoices");  setActiveNav(navInvoices);  }
    @FXML private void onBanks()     { navigate("banks");     setActiveNav(navBanks);     }
    @FXML private void onProfile()   { navigate("profile");   setActiveNav(navProfile);   }
    @FXML private void onSettings()  { navigate("settings");  setActiveNav(navSettings);  }
    @FXML private void onSupport()   { navigate("support");   setActiveNav(navSupport);   }

    /**
     * Navigate to a named page.
     *
     * FIX: The animation is now self-contained here.
     *   - 'view' opacity is set to 0 BEFORE it is added to the scene.
     *   - The old node is faded out first; only inside setOnFinished() do we
     *     call contentPane.getChildren().setAll(view) — at which point the
     *     old node is no longer being animated, so detachment is safe and
     *     setOnFinished() always fires correctly.
     */
    public void navigate(String page) {
        String fxmlPath = fxmlMap.get(page);
        if (fxmlPath == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            // Hide the incoming view immediately so it doesn't flash at full opacity
            view.setOpacity(0);

            // Inject MainController reference into sub-controllers that need it
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController dc) dc.setMainController(this);
            if (ctrl instanceof ChequeController    cc) cc.setMainController(this);
            if (ctrl instanceof InvoiceController   ic) ic.setMainController(this);

            Node current = contentPane.getChildren().isEmpty()
                ? null
                : contentPane.getChildren().get(0);

            if (current != null) {
                // --- FIX: inline animation; swap ONLY after fade-out finishes ---
                FadeTransition fadeOut = new FadeTransition(Duration.millis(140), current);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    // Safe to detach 'current' now — its animation is fully done
                    contentPane.getChildren().setAll(view);

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(220), view);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                // First load — no outgoing node, just fade the new view in
                contentPane.getChildren().setAll(view);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), view);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }

            headerTitle.setText(titleMap.getOrDefault(page, "ChequePro"));

        } catch (IOException e) {
            e.printStackTrace();
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