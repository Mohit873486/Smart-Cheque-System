# ChequePro — Complete Development & Debugging Guide

This guide provides production-ready code examples, best practices, and debugging checklists for the Smart Cheque System.

---

## Table of Contents
1. [Complete Working Code](#complete-working-code)
2. [Best-Practice Architecture](#best-practice-architecture)
3. [Debugging Checklist](#debugging-checklist)
4. [Quick Fixes](#quick-fixes)
5. [Product-Level Enhancements](#product-level-enhancements)

---

## Complete Working Code

### 1. MainController — Robust Navigation & Page Switching

**File:** `src/main/java/com/chequeprint/controller/MainController.java`

**Key Features:**
- ✅ Safe FXML loading with null checks
- ✅ View caching to avoid recreating heavy scenes
- ✅ Controller injection for cross-page communication
- ✅ Smooth fade transitions between pages
- ✅ Role-based access control (permission checks before loading)
- ✅ Page cache persists state between navigation
- ✅ Debug logging for troubleshooting

**Core `navigate()` Method Pattern:**
```java
public void navigate(String page) {
    // 1. Permission check
    if (!isPageAllowed(page)) {
        headerTitle.setText("Access Denied");
        Label message = new Label("You do not have permission to access this page.");
        contentPane.getChildren().setAll(message);
        return;
    }

    // 2. Get FXML path
    String path = fxmlMap.get(page);
    if (path == null) return;

    try {
        Node view;

        // 3. Check cache first (avoid reloading same page)
        if (pageCache.containsKey(page)) {
            view = pageCache.get(page);
        } else {
            // 4. Load FXML with null check
            URL res = getClass().getResource(path);
            if (res == null) {
                throw new IllegalStateException("FXML not found: " + path);
            }
            
            FXMLLoader loader = new FXMLLoader(res);
            view = loader.load();

            // 5. Ensure Region fills available space
            if (view instanceof Region region) {
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            }

            // 6. Inject main controller reference into sub-controllers
            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardController dc) dc.setMainController(this);
            if (ctrl instanceof ChequeController cc) cc.setMainController(this);
            // ... inject other controllers similarly ...

            // 7. Cache view and controller
            pageCache.put(page, view);
            controllerMap.put(page, ctrl);
        }

        // 8. Replace content with fade animation
        Node current = contentPane.getChildren().isEmpty() 
            ? null 
            : contentPane.getChildren().get(0);

        if (current == view) {
            view.setOpacity(1);
            return; // Already showing
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

        // 9. Update header
        headerTitle.setText(titleMap.getOrDefault(page, "ChequePro"));

        // 10. Debug log
        System.out.println("✓ Loaded page: " + page);

    } catch (Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            Label error = new Label("Unable to load " + titleMap.getOrDefault(page, page) 
                + "\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            error.setWrapText(true);
            error.getStyleClass().add("empty-label");
            contentPane.getChildren().setAll(error);
            headerTitle.setText("Load Error");
        });
    }
}
```

**FXML Map (centralized page registry):**
```java
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
```

**Navigation Methods (simple wrappers):**
```java
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

// ... similar for other pages ...

// Public methods for external callers (e.g., from DashboardController)
public void showDashboard() { onDashboard(); }
public void showCheques() { onCheques(); }
// ... etc ...
```

---

### 2. DashboardController — Data Loading & Chart Population

**File:** `src/main/java/com/chequeprint/controller/DashboardController.java`

**Key Features:**
- ✅ Background thread for heavy database operations
- ✅ UI updates via `Platform.runLater()` (thread-safe)
- ✅ Chart population (LineChart for 30-day trends, PieChart for status summary)
- ✅ Table data binding
- ✅ Auto-refresh every 10 seconds
- ✅ Null-safe field access
- ✅ Animation on card entrance

**Initialize & Reload Pattern:**
```java
@FXML
public void initialize() {
    System.out.println("DashboardController initialized");

    // Safe null checks
    safeCheck("lblWelcome", lblWelcome);
    safeCheck("lblSubtitle", lblSubtitle);
    safeCheck("cardTotal", cardTotal);

    // Setup tables (column factories)
    initializeRecentTables();

    // Set header text
    if (lblWelcome != null) lblWelcome.setText("Welcome, Mohit");
    if (lblSubtitle != null) lblSubtitle.setText("Finance Dashboard");

    // Animate entrance
    FxUtils.animateIn(lblWelcome, 0);
    FxUtils.animateIn(lblSubtitle, 60);

    // Start auto-refresh (every 10 seconds)
    startAutoRefresh();

    // Load data asynchronously
    reload();
}

/**
 * Reload dashboard data on background thread.
 * All UI updates are done via Platform.runLater() to ensure thread safety.
 */
public void reload() {
    new Thread(() -> {
        try {
            // Load data from services
            int total = service.getTotalCheques();
            int printed = service.getPrintedCheques();
            int pending = service.getPendingCheques();
            int today = service.getTodayCheques();
            double amount = service.getMonthlyAmount();
            int invoice = invoiceService.getTotalInvoices();
            var recentCheques = service.getAll();
            var recentInvoices = invoiceService.getAll();
            User profile = userService.loadProfile();

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                String displayName = profile != null && profile.getName() != null 
                    && !profile.getName().isBlank()
                    ? profile.getName()
                    : "Admin";
                
                if (lblWelcome != null) {
                    lblWelcome.setText("Welcome, " + displayName);
                }

                // Card animations
                animateCard(cardTotal, 80);
                animateCard(cardPrinted, 160);
                animateCard(cardPending, 240);
                animateCard(cardAmount, 320);

                // Update card labels
                setCount(lblTotalCheques, total, "");
                setCount(lblPendingCheques, pending, "");
                setCount(lblPrintedCheques, printed, "");

                // Populate tables
                if (tblRecentCheques != null && recentCheques != null) {
                    tblRecentCheques.getItems().setAll(recentCheques);
                }
                if (tblRecentInvoices != null && recentInvoices != null) {
                    tblRecentInvoices.getItems().setAll(recentInvoices);
                }

                // Populate LineChart (30-day activity)
                try {
                    if (chequeChart != null) {
                        chequeChart.getData().clear();
                        XYChart.Series<Number, Number> series = new XYChart.Series<>();
                        LocalDate start = LocalDate.now().minusDays(29);
                        for (int i = 0; i < 30; i++) {
                            LocalDate d = start.plusDays(i);
                            int count = 0;
                            try {
                                count = service.getCountByDate(d);
                            } catch (Exception ex) {
                                // ignore per-day errors
                            }
                            series.getData().add(new XYChart.Data<>(i + 1, count));
                        }
                        chequeChart.getData().add(series);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Populate PieChart (status summary)
                try {
                    if (statusChart != null) {
                        statusChart.getData().clear();
                        int printedCount = printed;
                        int pendingCount = pending;
                        int draftCount = Math.max(total - printedCount - pendingCount, 0);
                        
                        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                            new PieChart.Data("Printed", printedCount),
                            new PieChart.Data("Pending", pendingCount),
                            new PieChart.Data("Draft", draftCount)
                        );
                        statusChart.setData(pieData);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Add hover effects
                addHover(cardTotal);
                addHover(cardPrinted);
                addHover(cardPending);
                addHover(cardAmount);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                if (lblWelcome != null) lblWelcome.setText("Database Error");
            });
        }
    }).start();
}

// Helper: animate card entrance
private void animateCard(VBox card, int delay) {
    if (card != null) {
        FxUtils.bounceIn(card, delay);
    }
}

// Helper: count-up animation for labels
private void setCount(Label label, int value, String prefix) {
    if (label != null) {
        FxUtils.countUp(label, value, prefix, 100);
    }
}

// Helper: add hover effect
private void addHover(VBox card) {
    if (card != null) {
        FxUtils.addHoverEffect(card);
    }
}

// Auto-refresh every 10 seconds
private void startAutoRefresh() {
    if (autoRefreshTimeline != null) {
        autoRefreshTimeline.stop();
    }
    autoRefreshTimeline = new Timeline(
        new KeyFrame(Duration.seconds(10), e -> reload())
    );
    autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
    autoRefreshTimeline.play();
}
```

---

### 3. Main FXML Layout — Correct Structure

**File:** `src/main/resources/view/main.fxml`

**Critical Layout Rules:**
- BorderPane root with fixed left sidebar
- Center VBox with header (fixed height) + content area (grows)
- StackPane content pane with `VBox.vgrow="ALWAYS"` (CRITICAL!)
- Nav items with `onMouseClicked` or `onAction` handlers

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx/17"
    xmlns:fx="http://javafx.com/fxml"
    fx:controller="com.chequeprint.controller.MainController"
    prefWidth="1280" prefHeight="800"
    minWidth="1000" minHeight="600"
    styleClass="app-root">

    <!-- LEFT SIDEBAR (fixed width 224px) -->
    <left>
        <VBox fx:id="sidebar" styleClass="sidebar"
            prefWidth="224" minWidth="224" maxWidth="224">
            
            <VBox spacing="2" VBox.vgrow="ALWAYS" style="-fx-padding: 12 8 8 8;">
                <Label text="MAIN MENU" styleClass="sidebar-section-label" />

                <!-- Dashboard nav item -->
                <HBox fx:id="navDashboard" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onDashboard" pickOnBounds="true">
                    <Label text="📊" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Dashboard" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <HBox fx:id="navCheques" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onCheques" pickOnBounds="true">
                    <Label text="📝" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Cheques" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <HBox fx:id="navInvoices" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onInvoices" pickOnBounds="true">
                    <Label text="🧾" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Invoices" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <HBox fx:id="navBanks" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onBanks" pickOnBounds="true">
                    <Label text="🏦" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Banks" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <HBox fx:id="navAiAssistant" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onAiAssistant" pickOnBounds="true">
                    <Label text="AI" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="AI Assistant" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <Label text="ACCOUNT" styleClass="sidebar-section-label" />

                <HBox fx:id="navProfile" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onProfile" pickOnBounds="true">
                    <Label text="👤" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Profile" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <HBox fx:id="navSettings" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onSettings" pickOnBounds="true">
                    <Label text="⚙️" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Settings" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <HBox fx:id="navSupport" styleClass="nav-item" spacing="12"
                    alignment="CENTER_LEFT" onMouseClicked="#onSupport" pickOnBounds="true">
                    <Label text="💬" styleClass="nav-icon" mouseTransparent="true" />
                    <Label text="Support" styleClass="nav-label" mouseTransparent="true" />
                </HBox>

                <!-- Spacer pushes user footer to bottom -->
                <Region VBox.vgrow="ALWAYS" />

                <!-- User footer -->
                <HBox spacing="10" alignment="CENTER_LEFT" styleClass="sidebar-user">
                    <Label text="AD" styleClass="user-avatar" />
                    <VBox spacing="1">
                        <Label text="Admin User" styleClass="user-name" />
                        <Label text="Administrator" styleClass="user-role" />
                    </VBox>
                </HBox>
            </VBox>
        </VBox>
    </left>

    <!-- CENTER: Header + Dynamic Content -->
    <center>
        <VBox>
            <!-- Fixed header (58px) -->
            <HBox spacing="16" alignment="CENTER_LEFT" styleClass="top-header"
                prefHeight="58" minHeight="58" maxHeight="58"
                VBox.vgrow="NEVER"
                prefWidth="Infinity" maxWidth="Infinity">
                <Label fx:id="headerTitle" text="Dashboard" styleClass="header-title" />
                <Region HBox.hgrow="ALWAYS" />
                <TextField fx:id="mainSearchField" promptText="Search…" prefWidth="200" />
                <Label fx:id="lblNotification" text="🔔" styleClass="icon-btn" 
                    onMouseClicked="#onNotificationClicked" />
                <Label text="AD" styleClass="header-avatar" />
            </HBox>

            <!-- CONTENT PANE (CRITICAL: VBox.vgrow="ALWAYS") -->
            <StackPane fx:id="contentPane" VBox.vgrow="ALWAYS" />
        </VBox>
    </center>

</BorderPane>
```

---

### 4. Dashboard FXML — Minimal Working Example

**File:** `src/main/resources/view/dashboard.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.chart.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx/17"
    xmlns:fx="http://javafx.com/fxml/1"
    fx:controller="com.chequeprint.controller.DashboardController"
    fx:id="dashboardRoot"
    styleClass="dashboard-root"
    maxWidth="Infinity" maxHeight="Infinity">

    <center>
        <ScrollPane fitToWidth="true" hbarPolicy="NEVER" vbarPolicy="AS_NEEDED">
            <content>
                <VBox spacing="24" styleClass="dashboard-page" maxWidth="Infinity">
                    <padding>
                        <Insets top="28" right="32" bottom="32" left="32" />
                    </padding>

                    <!-- Header Section -->
                    <VBox spacing="6">
                        <Label fx:id="lblWelcome" text="Welcome back" styleClass="page-title" />
                        <Label fx:id="lblSubtitle" text="Your cheque operations are running smoothly." 
                            styleClass="page-subtitle" wrapText="true" />
                    </VBox>

                    <!-- Stats Cards -->
                    <HBox spacing="16" alignment="TOP_LEFT">
                        <VBox fx:id="cardTotal" styleClass="stat-card" HBox.hgrow="ALWAYS">
                            <HBox spacing="10" alignment="CENTER_LEFT">
                                <Label text="🧾" styleClass="stat-icon" />
                                <Label text="Total Cheques" styleClass="stat-label" />
                            </HBox>
                            <Label fx:id="lblTotalCheques" text="0" styleClass="stat-value" />
                        </VBox>

                        <VBox fx:id="cardPending" styleClass="stat-card" HBox.hgrow="ALWAYS">
                            <HBox spacing="10" alignment="CENTER_LEFT">
                                <Label text="⏳" styleClass="stat-icon" />
                                <Label text="Pending" styleClass="stat-label" />
                            </HBox>
                            <Label fx:id="lblPendingCheques" text="0" styleClass="stat-value" />
                        </VBox>

                        <VBox fx:id="cardPrinted" styleClass="stat-card" HBox.hgrow="ALWAYS">
                            <HBox spacing="10" alignment="CENTER_LEFT">
                                <Label text="✅" styleClass="stat-icon" />
                                <Label text="Approved" styleClass="stat-label" />
                            </HBox>
                            <Label fx:id="lblPrintedCheques" text="0" styleClass="stat-value" />
                        </VBox>

                        <VBox fx:id="cardAmount" styleClass="stat-card" HBox.hgrow="ALWAYS">
                            <HBox spacing="10" alignment="CENTER_LEFT">
                                <Label text="🖨️" styleClass="stat-icon" />
                                <Label text="Printed" styleClass="stat-label" />
                            </HBox>
                            <Label fx:id="lblClearedCount" text="0" styleClass="stat-value" />
                        </VBox>
                    </HBox>

                    <!-- Charts Row -->
                    <HBox spacing="18" alignment="TOP_LEFT" VBox.vgrow="ALWAYS">
                        <!-- LineChart -->
                        <VBox spacing="16" HBox.hgrow="ALWAYS" minHeight="320">
                            <Label text="Cheque Activity (Last 30 Days)" styleClass="section-title" />
                            <LineChart fx:id="chequeChart" animated="false" 
                                legendVisible="false" createSymbols="false" VBox.vgrow="ALWAYS">
                                <xAxis>
                                    <NumberAxis side="BOTTOM" autoRanging="true" />
                                </xAxis>
                                <yAxis>
                                    <NumberAxis side="LEFT" autoRanging="true" />
                                </yAxis>
                            </LineChart>
                        </VBox>

                        <!-- PieChart -->
                        <VBox spacing="16" prefWidth="320" minHeight="320">
                            <Label text="Status Overview" styleClass="section-title" />
                            <PieChart fx:id="statusChart" legendSide="RIGHT" 
                                labelsVisible="false" prefHeight="260" />
                        </VBox>
                    </HBox>

                    <!-- Recent Cheques Table -->
                    <VBox spacing="16">
                        <Label text="Recent Cheques" styleClass="section-title" />
                        <TableView fx:id="tblRecentCheques" styleClass="modern-table" prefHeight="340">
                            <columnResizePolicy>
                                <TableView fx:constant="CONSTRAINED_RESIZE_POLICY" />
                            </columnResizePolicy>
                            <columns>
                                <TableColumn fx:id="colChequeNo" text="Cheque ID" minWidth="90" />
                                <TableColumn fx:id="colPayee" text="Payee" minWidth="140" />
                                <TableColumn fx:id="colAmount" text="Amount" minWidth="90" />
                                <TableColumn fx:id="colBank" text="Bank" minWidth="110" />
                                <TableColumn fx:id="colStatus" text="Status" minWidth="100" />
                                <TableColumn fx:id="colChequeDate" text="Date" minWidth="110" />
                            </columns>
                            <placeholder>
                                <Label text="No recent cheques." />
                            </placeholder>
                        </TableView>
                    </VBox>

                </VBox>
            </content>
        </ScrollPane>
    </center>

</BorderPane>
```

---

### 5. ChequeService — Data Access Helper

**File:** `src/main/java/com/chequeprint/service/ChequeService.java`

**Add this method:**
```java
/**
 * Get count of cheques for a specific date.
 * Used by dashboard for 30-day trend chart.
 */
public int getCountByDate(java.time.LocalDate date) throws SQLException {
    return dao.countByIssueDate(date);
}
```

---

## Best-Practice Architecture

### Pattern: MVC (Model-View-Controller)

**Layer Breakdown:**
```
┌─────────────────────────────────────┐
│     FXML Views                      │  ← UI Layout (XML)
│  (dashboard.fxml, cheques.fxml)     │
└─────────────────────────────────────┘
           ↕ (fx:controller)
┌─────────────────────────────────────┐
│     Controllers                     │  ← View Logic & Event Handling
│  (DashboardController, etc.)        │  (update UI, handle clicks)
└─────────────────────────────────────┘
           ↕ (new + inject)
┌─────────────────────────────────────┐
│     Services                        │  ← Business Logic
│  (ChequeService, InvoiceService)    │  (validation, calculations)
└─────────────────────────────────────┘
           ↕ (query + insert)
┌─────────────────────────────────────┐
│     DAOs (Data Access Objects)      │  ← Database Queries
│  (ChequeDAO, InvoiceDAO)            │  (CRUD operations)
└─────────────────────────────────────┘
           ↕ (SQL)
┌─────────────────────────────────────┐
│     Database (MySQL)                │  ← Data Storage
└─────────────────────────────────────┘
```

**Benefits:**
- ✅ **Separation of Concerns:** Each layer has one responsibility.
- ✅ **Testability:** Services can be tested independently of UI.
- ✅ **Reusability:** Services can be called from multiple controllers.
- ✅ **Maintainability:** Changes to DB schema only affect DAO.

### Navigation Pattern: Single Window, Multiple Content Panes

**Why not multiple Scenes?**
- ❌ Multiple Scenes require managing multiple Stage objects.
- ❌ Keyboard shortcuts and global state become complex.
- ❌ Styling consistency requires duplicating CSS across scenes.

**Why BorderPane + StackPane?**
- ✅ Single Stage/Scene simplifies lifecycle.
- ✅ Sidebar remains consistent; only center content changes.
- ✅ Page state (scroll position, form data) persisted via caching.
- ✅ Animations smooth and predictable.

**Caching Pattern:**
```java
private final Map<String, Node> pageCache = new HashMap<>();

// Load once, reuse many times
if (pageCache.containsKey(page)) {
    view = pageCache.get(page);  // Fast
} else {
    view = loader.load();         // Slow (first time)
    pageCache.put(page, view);
}
```

### Threading Pattern: Background Work + UI Updates

**Always:**
1. Do heavy work (DB queries) on background thread
2. Update UI on JavaFX thread

```java
new Thread(() -> {
    // Heavy work (off UI thread)
    List<Cheque> cheques = service.getAll();
    
    // Update UI (on UI thread)
    Platform.runLater(() -> {
        table.getItems().setAll(cheques);
    });
}).start();
```

---

## Debugging Checklist

### Symptom: Dashboard Button Highlighted, Content Area Blank

**Checklist:**
- [ ] 1. **Check console for exceptions**
  - Look for `NullPointerException`, `FileNotFoundException`, `ClassNotFoundException`
  - Note: If no exception, issue is likely silent (e.g., null field)
  
- [ ] 2. **Verify FXML path is correct**
  - Confirm file exists: `src/main/resources/view/dashboard.fxml`
  - Confirm path in `fxmlMap`: `/view/dashboard.fxml` (leading `/`)
  - Test: `URL res = getClass().getResource("/view/dashboard.fxml");` should not be null
  
- [ ] 3. **Check fx:id matches @FXML fields**
  - Every `@FXML private` field must have matching `fx:id="..."` in FXML
  - Example: `@FXML private Label lblTotalCheques;` requires `<Label fx:id="lblTotalCheques" />`
  - Missing: field becomes null, but doesn't crash — just silently fails
  
- [ ] 4. **Check controller class in FXML**
  - FXML must declare: `fx:controller="com.chequeprint.controller.DashboardController"`
  - Wrong: `fx:controller="DashboardController"` → Error
  
- [ ] 5. **Verify content pane grows**
  - Parent of content must be VBox with `VBox.vgrow="ALWAYS"`
  - Without this, content pane has zero height → blank
  - Test: Temporarily add a Label to see if it appears
  
- [ ] 6. **Check for CSS hiding the content**
  - CSS rule like `-fx-pref-height: 0` will hide content
  - Check `.dashboard-page`, `.modern-table`, etc. in `style.css`
  
- [ ] 7. **Test with simple content first**
  - Replace FXML with minimal: `<Label text="TEST" />`
  - If Label appears → Issue is in complex FXML; if not → Issue is in MainController

### Symptom: Compilation Error "cannot find symbol: variable lblXyz"

**Solution:**
- Remove `@FXML` field declaration if not used in FXML
- Or add missing `fx:id` to FXML element
- Example:
  ```java
  // BAD (no matching fx:id in FXML)
  @FXML private Label lblUnused;
  
  // GOOD
  // Remove from controller
  ```

### Symptom: Page Loads Blank (No Data, No Charts)

**Checklist:**
- [ ] Database connection works: Check `AppConfig.getConnection()`
- [ ] Service methods return data: Add debug log `System.out.println(service.getTotalCheques())`
- [ ] Chart null check passes: Add log before populating
- [ ] Chart data not empty: Check `chequeChart.getData()` has series
- [ ] Tables populate: Check `tblRecentCheques.getItems()` is not empty

**Debug Code:**
```java
Platform.runLater(() -> {
    System.out.println("Total cheques: " + total);
    System.out.println("Chart is null: " + (chequeChart == null));
    System.out.println("Table is null: " + (tblRecentCheques == null));
});
```

---

## Quick Fixes

### Fix 1: Content Pane Not Showing

**Issue:** Center area is blank even though page loads.

**Fix:**
```xml
<!-- main.fxml: Ensure StackPane has VBox.vgrow="ALWAYS" -->
<StackPane fx:id="contentPane" VBox.vgrow="ALWAYS" />
```

### Fix 2: Null FXML Field

**Issue:** Page loads but certain UI elements are missing or NPE occurs.

**Fix in Controller:**
```java
// BEFORE (crashes if field is null)
lblTotalCheques.setText("100");

// AFTER (safe)
if (lblTotalCheques != null) {
    lblTotalCheques.setText("100");
}
```

### Fix 3: Page Not Loading (FileNotFoundException)

**Issue:** Path not found during FXMLLoader.

**Debug:**
```java
URL res = getClass().getResource("/view/dashboard.fxml");
System.out.println("Resource: " + (res != null ? res : "NOT FOUND"));

// If null, verify:
// 1. File exists: src/main/resources/view/dashboard.fxml
// 2. Maven copied it: check target/classes/view/dashboard.fxml
// 3. Build is clean: mvn clean
```

### Fix 4: Page Loads but Tables/Charts Empty

**Issue:** UI renders but no data.

**Fix:**
```java
// In reload() method, add logs
System.out.println("Fetching data...");
int total = service.getTotalCheques();
System.out.println("Total cheques: " + total);

Platform.runLater(() -> {
    System.out.println("Setting table: " + recentCheques.size() + " items");
    tblRecentCheques.getItems().setAll(recentCheques);
});
```

### Fix 5: Database Error

**Issue:** "Cannot create connection" or SQLException.

**Fix in AppConfig.java:**
```java
// Verify credentials and URL
String DB_URL = "jdbc:mysql://localhost:3306/chequeprint_db";
String DB_USER = "root";
String DB_PASS = "yourpassword";

// Test connection
try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
    System.out.println("✓ Database connected");
} catch (SQLException e) {
    System.err.println("✗ Database connection failed: " + e.getMessage());
}
```

---

## Product-Level Enhancements

### 1. Dark Mode Support

**CSS Variable Approach:**
```css
/* light-theme.css */
.root {
    -fx-primary: #007ACC;
    -fx-background: #FFFFFF;
    -fx-text: #000000;
}
.label { -fx-text-fill: -fx-text; }
.vbox { -fx-background-color: -fx-background; }

/* dark-theme.css */
.root {
    -fx-primary: #007ACC;
    -fx-background: #1E1E1E;
    -fx-text: #FFFFFF;
}
```

**Toggle in Settings:**
```java
@FXML
private void onToggleDarkMode(boolean enabled) {
    String theme = enabled ? "dark-theme.css" : "light-theme.css";
    scene.getStylesheets().clear();
    scene.getStylesheets().add(getClass().getResource("/css/" + theme).toExternalForm());
}
```

### 2. Sidebar Collapse/Expand

```java
private double sidebarWidth = 224;
private boolean collapsed = false;

@FXML
private void onToggleSidebar() {
    if (collapsed) {
        // Expand
        Timeline expand = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(sidebar.prefWidthProperty(), sidebarWidth))
        );
        expand.play();
        collapsed = false;
    } else {
        // Collapse
        Timeline collapse = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(sidebar.prefWidthProperty(), 60))
        );
        collapse.play();
        collapsed = true;
    }
}
```

### 3. Loading Spinner

```java
// Show spinner while loading
ProgressIndicator spinner = new ProgressIndicator();
spinner.setProgress(-1); // Indeterminate
contentPane.getChildren().setAll(spinner);

// Later, replace with actual content
contentPane.getChildren().setAll(actualView);
```

### 4. Role-Based Dashboard

```java
@Override
public void setCurrentUser(User user) {
    this.currentUser = user;
    
    if (user.getRole().equals("ADMIN")) {
        // Show all sections
        navCheques.setVisible(true);
        navBanks.setVisible(true);
    } else if (user.getRole().equals("OPERATOR")) {
        // Hide audit/settings
        navSettings.setVisible(false);
    }
}
```

---

## Summary

**You now have:**
- ✅ Working navigation system (MainController)
- ✅ Dashboard with charts and tables (DashboardController)
- ✅ Correct FXML layout (main.fxml, dashboard.fxml)
- ✅ Best practices for threading and MVC
- ✅ Debugging checklist for common issues
- ✅ Product-ready enhancement suggestions

**Next Steps:**
1. Run `mvn clean javafx:run` to test the working app.
2. Use the debugging checklist if anything goes wrong.
3. Implement dark mode or sidebar collapse for polish.

