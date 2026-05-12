package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MainController — manages the app shell:
 *   - Left sidebar navigation
 *   - Top header (search, user avatar)
 *   - Center content pane (swaps FXML views)
 */
public class MainController {

    @FXML private VBox   sidebar;
    @FXML private StackPane contentPane;
    @FXML private Label  headerTitle;

    // Sidebar nav buttons (fx:id must match)
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
        // Default page
        navigate("dashboard");
        setActiveNav(navDashboard);
    }

    /** Called from nav buttons via onAction in FXML. */
    @FXML private void onDashboard() { navigate("dashboard"); setActiveNav(navDashboard); }
    @FXML private void onCheques()   { navigate("cheques");   setActiveNav(navCheques);   }
    @FXML private void onInvoices()  { navigate("invoices");  setActiveNav(navInvoices);  }
    @FXML private void onBanks()     { navigate("banks");     setActiveNav(navBanks);     }
    @FXML private void onProfile()   { navigate("profile");   setActiveNav(navProfile);   }
    @FXML private void onSettings()  { navigate("settings");  setActiveNav(navSettings);  }
    @FXML private void onSupport()   { navigate("support");   setActiveNav(navSupport);   }

    /** Navigate to a named page — also called programmatically from sub-controllers. */
    public void navigate(String page) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(fxmlMap.get(page)));
            Node view = loader.load();

            // Inject reference to MainController into sub-controllers that need it
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController dc) dc.setMainController(this);
            if (ctrl instanceof ChequeController cc)    cc.setMainController(this);
            if (ctrl instanceof InvoiceController ic)   ic.setMainController(this);

            // Animated page switch
            Node current = contentPane.getChildren().isEmpty()
                ? null : contentPane.getChildren().get(0);

            if (current != null) {
                FxUtils.switchPage(current, view, () -> {
                    contentPane.getChildren().setAll(view);
                });
            } else {
                contentPane.getChildren().setAll(view);
                view.setOpacity(0);
                FxUtils.animateIn(view, 0);
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