package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML
    private VBox sidebar;

    @FXML
    private StackPane contentPane;

    @FXML
    private Label headerTitle;

    // Navigation items
    @FXML private HBox navDashboard;
    @FXML private HBox navCheques;
    @FXML private HBox navInvoices;
    @FXML private HBox navBanks;
    @FXML private HBox navProfile;
    @FXML private HBox navSettings;
    @FXML private HBox navSupport;

    private HBox activeNavItem;

    // FXML mapping (MAIN SYSTEM)
    private final Map<String, String> fxmlMap = new HashMap<>() {{
        put("dashboard", "/view/dashboard.fxml");
        put("cheques", "/view/cheques.fxml");
        put("invoices", "/view/invoices.fxml");
        put("banks", "/view/banks.fxml");
        put("profile", "/view/profile.fxml");
        put("settings", "/view/settings.fxml");
        put("support", "/view/support.fxml");
    }};

    // Titles
    private final Map<String, String> titleMap = new HashMap<>() {{
        put("dashboard", "Dashboard");
        put("cheques", "Cheque Management");
        put("invoices", "Invoice Management");
        put("banks", "Bank Templates");
        put("profile", "My Profile");
        put("settings", "Settings");
        put("support", "Support Center");
    }};

    @FXML
    public void initialize() {

        // Sidebar animation
        int delay = 0;
        for (Node child : sidebar.getChildren()) {
            FxUtils.animateIn(child, delay);
            delay += 40;
        }

        // Default page
        navigate("dashboard");
        setActiveNav(navDashboard);
    }

    // ================= NAVIGATION METHODS =================

    @FXML private void onDashboard() { navigate("dashboard"); setActiveNav(navDashboard); }
    @FXML private void onCheques()   { navigate("cheques");   setActiveNav(navCheques); }
    @FXML private void onInvoices()  { navigate("invoices");  setActiveNav(navInvoices); }
    @FXML private void onBanks()     { navigate("banks");     setActiveNav(navBanks); }
    @FXML private void onProfile()   { navigate("profile");   setActiveNav(navProfile); }
    @FXML private void onSettings()  { navigate("settings");  setActiveNav(navSettings); }
    @FXML private void onSupport()   { navigate("support");   setActiveNav(navSupport); }

    // ================= MAIN NAVIGATION (ADVANCED) =================

    public void navigate(String page) {
        String path = fxmlMap.get(page);
        if (path == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node view = loader.load();

            view.setOpacity(0); // no flash

            // Inject controller (optional)
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController dc) dc.setMainController(this);
            if (ctrl instanceof ChequeController cc) cc.setMainController(this);
            if (ctrl instanceof InvoiceController ic) ic.setMainController(this);
            if (ctrl instanceof ProfileController pc) pc.setMainController(this);
            if (ctrl instanceof SettingsController pc) pc.setMainController(this);
            if (ctrl instanceof SupportController pc) pc.setMainController(this);

            Node current = contentPane.getChildren().isEmpty()
                    ? null
                    : contentPane.getChildren().get(0);

            if (current != null) {

                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), current);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                fadeOut.setOnFinished(e -> {
                    contentPane.getChildren().setAll(view);

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(250), view);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });

                fadeOut.play();

            } else {
                contentPane.getChildren().setAll(view);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), view);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }

            headerTitle.setText(titleMap.getOrDefault(page, "ChequePro"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SIMPLE LOADER (EXTRA SUPPORT) =================

    public void loadPageSimple(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFile));
            contentPane.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ACTIVE NAV STYLE =================

    private void setActiveNav(HBox item) {
        if (activeNavItem != null)
            activeNavItem.getStyleClass().remove("nav-active");

        activeNavItem = item;

        if (item != null)
            item.getStyleClass().add("nav-active");
    }
}