package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.ChequeReminderService;
import com.chequeprint.util.FxUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

  @FXML
  private TextField mainSearchField;

  @FXML
  private Label lblNotification;

  // Navigation items
  @FXML
  private HBox navDashboard;
  @FXML
  private HBox navCheques;
  @FXML
  private HBox navInvoices;
  @FXML
  private HBox navBanks;
  @FXML
  private HBox navAiAssistant;
  @FXML
  private HBox navProfile;
  @FXML
  private HBox navSettings;
  @FXML
  private HBox navSupport;

  private HBox activeNavItem;
  private User currentUser;

  // FXML mapping (MAIN SYSTEM)
  private final Map<String, String> fxmlMap = new HashMap<>() {
    {
      put("dashboard", "/view/dashboard.fxml");
      put("cheques", "/view/cheques.fxml");
      put("invoices", "/view/invoices.fxml");
      put("banks", "/view/banks.fxml");
      put("ai", "/view/ai-assistant.fxml");
      put("profile", "/view/profile.fxml");
      put("settings", "/view/settings.fxml");
      put("support", "/view/support.fxml");
    }
  };

  // Keep a reference to loaded controllers so other pages can be notified
  private final Map<String, Object> controllerMap = new HashMap<>();

  // Titles
  private final Map<String, String> titleMap = new HashMap<>() {
    {
      put("dashboard", "Dashboard");
      put("cheques", "Cheque Management");
      put("invoices", "Invoice Management");
      put("banks", "Bank Templates");
      put("ai", "AI Assistant");
      put("profile", "My Profile");
      put("settings", "Settings");
      put("support", "Support Center");
    }
  };

  private final Map<String, Node> pageCache = new HashMap<>();

  @FXML
  public void initialize() {

    // Sidebar animation
    int delay = 0;
    for (Node child : sidebar.getChildren()) {
      FxUtils.animateIn(child, delay);
      delay += 40;
    }

    if (mainSearchField != null) {
      mainSearchField.textProperty().addListener((obs, oldValue, newValue) -> handleMainSearch(newValue));
    }

    // The landing page is selected after the authenticated user is injected.
  }

  // ================= NAVIGATION METHODS =================

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
  private void onAiAssistant() {
    navigate("ai");
    setActiveNav(navAiAssistant);
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

  public void showDashboard() {
    onDashboard();
  }

  public void showCheques() {
    onCheques();
  }

  public void showInvoices() {
    onInvoices();
  }

  public void showBanks() {
    onBanks();
  }

  public void showAiAssistant() {
    onAiAssistant();
  }

  public void showProfile() {
    onProfile();
  }

  public void showSettings() {
    onSettings();
  }

  public void showSupport() {
    onSupport();
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
    applyRolePermissions();
    navigateRoleLanding();
  }

  private void navigateRoleLanding() {
    String[] preferredPages = {"dashboard", "cheques", "invoices", "banks", "profile", "support"};
    for (String page : preferredPages) {
      if (isPageAllowed(page)) {
        navigate(page);
        setActiveNav(navItemFor(page));
        return;
      }
    }
    navigate("support");
    setActiveNav(navSupport);
  }

  private void applyRolePermissions() {
    setNavAllowed(navDashboard, "dashboard");
    setNavAllowed(navCheques, "cheques");
    setNavAllowed(navInvoices, "invoices");
    setNavAllowed(navBanks, "banks");
    setNavAllowed(navAiAssistant, "ai");
    setNavAllowed(navProfile, "profile");
    setNavAllowed(navSettings, "settings");
    setNavAllowed(navSupport, "support");
  }

  private boolean isPageAllowed(String page) {
    return currentUser != null && AccessControl.canAccessPage(currentUser, page);
  }

  private void setNavAllowed(HBox item, String page) {
    boolean allowed = isPageAllowed(page);
    item.setVisible(allowed);
    item.setManaged(allowed);
  }

  private HBox navItemFor(String page) {
    return switch (page) {
      case "cheques" -> navCheques;
      case "invoices" -> navInvoices;
      case "banks" -> navBanks;
      case "ai" -> navAiAssistant;
      case "profile" -> navProfile;
      case "settings" -> navSettings;
      case "support" -> navSupport;
      default -> navDashboard;
    };
  }

  /**
   * Returns the controller instance for a cached page, or null if not loaded.
   */
  public Object getController(String page) {
    return controllerMap.get(page);
  }

  // ================= MAIN NAVIGATION (ADVANCED) =================

  public void navigate(String page) {

    if (!isPageAllowed(page)) {
      headerTitle.setText("Access Denied");
      Label message = new Label("You do not have permission to access this page.");
      message.setWrapText(true);
      message.getStyleClass().add("empty-label");
      contentPane.getChildren().setAll(message);
      return;
    }

    String path = fxmlMap.get(page);
    if (path == null)
      return;

    try {
      Node view;

      // ✅ CACHE FIX (IMPORTANT)
      if (pageCache.containsKey(page)) {
        view = pageCache.get(page);
      } else {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        view = loader.load();

        view.setOpacity(0);

        // Inject controller
        Object ctrl = loader.getController();
        if (ctrl instanceof DashboardController dc)
          dc.setMainController(this);
        if (ctrl instanceof ChequeController cc)
          cc.setMainController(this);
        if (ctrl instanceof InvoiceController ic)
          ic.setMainController(this);
        if (ctrl instanceof AiAssistantController ac)
          ac.setMainController(this);
        if (ctrl instanceof ProfileController pc)
          pc.setMainController(this);
        if (ctrl instanceof SettingsController sc)
          sc.setMainController(this);
        if (ctrl instanceof SupportController sc)
          sc.setMainController(this);

        // store in cache
        pageCache.put(page, view);
        // also keep controller reference for cross-page notifications
        controllerMap.put(page, ctrl);
      }

      Node current = contentPane.getChildren().isEmpty()
          ? null
          : contentPane.getChildren().get(0);

      if (current == view) {
        return; // ✅ avoid reloading same page
      }

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

      // ✅ DEBUG (optional)
      System.out.println("Loaded page: " + page);

    } catch (Exception e) {
      e.printStackTrace();
      Label error = new Label("Unable to load " + titleMap.getOrDefault(page, page)
          + ".\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
      error.setWrapText(true);
      error.getStyleClass().add("empty-label");
      contentPane.getChildren().setAll(error);
      headerTitle.setText("Load Error");
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

  @FXML
  private void onNotificationClicked() {
    if (lblNotification == null) {
      return;
    }
    try {
      ChequeReminderService reminderService = new ChequeReminderService();
      var cheques = reminderService.getUpcomingChequesWithinDays(2);
      String message = reminderService.buildReminderMessage(cheques);
      if (message.isBlank()) {
        message = "No notifications at the moment.";
      }
      Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
      alert.setTitle("Notifications");
      alert.setHeaderText("Upcoming cheque reminders");
      alert.showAndWait();
    } catch (Exception e) {
      Alert alert = new Alert(Alert.AlertType.ERROR, "Could not load notifications: " + e.getMessage());
      alert.setTitle("Notifications");
      alert.setHeaderText("Error");
      alert.showAndWait();
    }
  }

  private void handleMainSearch(String query) {
    Object controller = getCurrentController();
    if (controller instanceof ChequeController cc) {
      cc.applyMainSearch(query);
    } else if (controller instanceof InvoiceController ic) {
      ic.applyMainSearch(query);
    }
  }

  private Object getCurrentController() {
    if (contentPane == null || contentPane.getChildren().isEmpty()) {
      return null;
    }
    Node current = contentPane.getChildren().get(0);
    for (Map.Entry<String, Node> entry : pageCache.entrySet()) {
      if (entry.getValue() == current) {
        return controllerMap.get(entry.getKey());
      }
    }
    return null;
  }
}
