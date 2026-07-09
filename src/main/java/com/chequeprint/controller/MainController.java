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
import com.chequeprint.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.input.InputEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {

  @FXML
  private VBox sidebar;

  @FXML
  private Label sidebarLogoText;

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
  private HBox navChequeHistory;
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

  @FXML
  private HBox userFooter;

  @FXML
  private javafx.scene.control.Label lblUserAvatar;

  @FXML
  private javafx.scene.control.Label lblUserName;

  @FXML
  private javafx.scene.control.Label lblUserRole;

  @FXML
  private javafx.scene.control.Button btnLogout;

  // FXML mapping (MAIN SYSTEM)
  private final Map<String, String> fxmlMap = new HashMap<>() {
    {
      put("dashboard", "/view/dashboard.fxml");
      put("cheques", "/view/cheques.fxml");
      put("history", "/view/cheque_history.fxml");
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
      put("history", "Cheque History");
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
  private void onChequeHistory() {
    navigate("history");
    setActiveNav(navChequeHistory);
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

  private Timeline sessionTimeoutTimeline;

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
    applyRolePermissions();
    updateUserFooter();
    navigateRoleLanding();
    startSessionTimeoutCheck();

    // Load and apply the theme asynchronously on startup
    Thread themeLoaderThread = new Thread(() -> {
        try {
            com.chequeprint.service.SettingService service = new com.chequeprint.service.SettingService();
            com.chequeprint.model.Settings settings = service.getSettings();
            if (settings != null) {
                Platform.runLater(() -> {
                    if (sidebarLogoText != null && settings.getAppName() != null && !settings.getAppName().isBlank()) {
                        sidebarLogoText.setText(settings.getAppName());
                    }
                    if (contentPane != null && contentPane.getScene() != null) {
                        com.chequeprint.util.ThemeManager.applyTheme(contentPane.getScene(), settings.getTheme());
                    }
                });
            }
        } catch (Exception ex) {
            System.err.println("[MainController] Error applying theme: " + ex.getMessage());
        }
    });
    themeLoaderThread.setDaemon(true);
    themeLoaderThread.start();
  }

  public void updateSidebarLogo(String appName) {
    if (sidebarLogoText != null) {
      sidebarLogoText.setText(appName != null && !appName.isBlank() ? appName : "ChequePro");
    }
  }

  private void startSessionTimeoutCheck() {
    if (sessionTimeoutTimeline != null) {
      sessionTimeoutTimeline.stop();
    }
    SessionManager.getInstance().updateActivity();

    Platform.runLater(() -> {
      if (contentPane != null && contentPane.getScene() != null) {
        contentPane.getScene().addEventFilter(InputEvent.ANY, e -> {
          SessionManager.getInstance().updateActivity();
        });
      }
    });

    sessionTimeoutTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> checkSessionTimeout()));
    sessionTimeoutTimeline.setCycleCount(Timeline.INDEFINITE);
    sessionTimeoutTimeline.play();
  }

  private void checkSessionTimeout() {
    if (SessionManager.getInstance().isExpired()) {
      if (sessionTimeoutTimeline != null) {
        sessionTimeoutTimeline.stop();
      }
      handleAutoLogout();
    }
  }

  private void handleAutoLogout() {
    Platform.runLater(() -> {
      try {
        if (sessionTimeoutTimeline != null) {
          sessionTimeoutTimeline.stop();
        }
        com.chequeprint.service.AuthService authService = new com.chequeprint.service.AuthService();
        authService.logout();
        SessionManager.getInstance().clear();

        for (Object ctrl : controllerMap.values()) {
          if (ctrl instanceof DashboardController dc) {
            dc.cleanup();
          }
        }
        controllerMap.clear();
        pageCache.clear();

        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
        Parent root = loader.load();
        javafx.stage.Stage stage = (javafx.stage.Stage) contentPane.getScene().getWindow();
        
        stage.setMaximized(false); // Restore normal window size
        
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 620);
        var stylesheet = getClass().getResource("/css/style.css");
        if (stylesheet != null) {
          scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        com.chequeprint.util.ThemeManager.applySavedTheme(scene);
        stage.setScene(scene);
        stage.setTitle("Smart Cheque Management System - Sign In");
        stage.centerOnScreen();

        Alert alert = new Alert(Alert.AlertType.WARNING, "Your session has expired due to inactivity. Please log in again.");
        alert.setTitle("Session Expired");
        alert.setHeaderText(null);
        alert.show();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private void updateUserFooter() {
    if (currentUser == null) {
      lblUserName.setText("-");
      lblUserRole.setText("");
      lblUserAvatar.setText("-");
      btnLogout.setVisible(false);
      btnLogout.setManaged(false);
      return;
    }
    String display = currentUser.getName() != null && !currentUser.getName().isBlank()
        ? currentUser.getName()
        : currentUser.getUsername();
    lblUserName.setText(display != null ? display : "User");
    lblUserRole.setText(currentUser.getRole() != null ? currentUser.getRole() : "");
    String avatar = display != null && display.length() > 0 ? display.substring(0, 1).toUpperCase() : "U";
    lblUserAvatar.setText(avatar);
    btnLogout.setVisible(true);
    btnLogout.setManaged(true);
  }

  @FXML
  private void onLogout() {
    try {
      javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
          "Are you sure you want to sign out?");
      confirm.setTitle("Sign out");
      var res = confirm.showAndWait();
      if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.OK) {
        return;
      }

      if (sessionTimeoutTimeline != null) {
        sessionTimeoutTimeline.stop();
      }

      // Perform logout
      com.chequeprint.service.AuthService authService = new com.chequeprint.service.AuthService();
      authService.logout();
      SessionManager.getInstance().clear();

      for (Object ctrl : controllerMap.values()) {
        if (ctrl instanceof DashboardController dc) {
          dc.cleanup();
        }
      }
      controllerMap.clear();
      pageCache.clear();

      // Load login scene
      javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
      javafx.scene.Parent root = loader.load();
      javafx.stage.Stage stage = (javafx.stage.Stage) contentPane.getScene().getWindow();
      
      stage.setMaximized(false); // Restore normal window size
      
      javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 620);
      var stylesheet = getClass().getResource("/css/style.css");
      if (stylesheet != null) {
        scene.getStylesheets().add(stylesheet.toExternalForm());
      }
      com.chequeprint.util.ThemeManager.applySavedTheme(scene);
      stage.setScene(scene);
      stage.setTitle("Smart Cheque Management System - Sign In");
      stage.centerOnScreen();
    } catch (Exception e) {
      e.printStackTrace();
      javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
          "Logout failed: " + e.getMessage());
      alert.showAndWait();
    }
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
    setNavAllowed(navChequeHistory, "history");
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
      case "history" -> navChequeHistory;
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
    if (SessionManager.getInstance().isExpired()) {
      handleAutoLogout();
      return;
    }

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
