package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.model.User;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.ChequeService;
import com.chequeprint.service.InvoiceService;
import com.chequeprint.service.Permission;
import com.chequeprint.service.UserService;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private BorderPane dashboardRoot;
    @FXML private Label lblWelcome;
    @FXML private Label lblSubtitle;
    @FXML private Label lblTotalCheques;
    @FXML private Label lblPendingCheques;
    @FXML private Label lblPrintedCheques;
    @FXML private Label lblClearedCount;

    @FXML private VBox cardTotal;
    @FXML private VBox cardPending;
    @FXML private VBox cardPrinted;
    @FXML private VBox cardAmount;

    @FXML private Button btnNewCheque;
    @FXML private Button btnNewInvoice;
    @FXML private Button btnViewAllCheques;
    @FXML private Button btnViewAllInvoices;
    @FXML private Button btnRefreshCheques;
    @FXML private Button btnQuickNewCheque;
    @FXML private Button btnQuickReviewCheques;
    @FXML private Button btnQuickOpenInvoices;
    @FXML private TextField txtChequeSearch;

    @FXML private LineChart<Number, Number> chequeChart;
    @FXML private PieChart statusChart;
    @FXML private Pane statusChartOverlay;
    @FXML private Label lblDonutTotal;
    @FXML private FlowPane legendContainer;

    @FXML private TableView<Cheque> tblRecentCheques;
    @FXML private TableColumn<Cheque, String> colChequeNo;
    @FXML private TableColumn<Cheque, String> colPayee;
    @FXML private TableColumn<Cheque, String> colAmount;
    @FXML private TableColumn<Cheque, String> colBank;
    @FXML private TableColumn<Cheque, String> colStatus;
    @FXML private TableColumn<Cheque, String> colChequeDate;

    @FXML private TableView<Invoice> tblRecentInvoices;
    @FXML private TableColumn<Invoice, String> colInvoiceNo;
    @FXML private TableColumn<Invoice, String> colInvoiceClient;
    @FXML private TableColumn<Invoice, String> colInvoiceAmount;
    @FXML private TableColumn<Invoice, String> colInvoiceDue;
    @FXML private TableColumn<Invoice, String> colInvoiceStatus;

    private final ChequeService chequeService = new ChequeService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final UserService userService = new UserService();

    private MainController mainController;
    private Timeline autoRefreshTimeline;
    private List<Cheque> loadedCheques = Collections.emptyList();
    private List<Invoice> loadedInvoices = Collections.emptyList();

    @FXML
    public void initialize() {
        configureRootSizing();
        configureTables();
        configureCharts();
        configureSearch();
        applyPermissions();
        showEmptyDashboard();
        animateInitialView();
        reload();
        startAutoRefresh();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        applyPermissions();
    }

    public void reload() {
        Thread worker = new Thread(() -> {
            DashboardData data = loadDashboardData();
            Platform.runLater(() -> applyDashboardData(data));
        }, "dashboard-reload");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onRefreshDashboard() {
        reload();
    }

    @FXML
    private void onNewCheque() {
        if (!can(Permission.CREATE_CHEQUE)) {
            showPermissionDenied();
            return;
        }
        if (mainController != null) {
            mainController.showCheques();
        }
    }

    @FXML
    private void onNewInvoice() {
        if (!can(Permission.VIEW_INVOICES)) {
            showPermissionDenied();
            return;
        }
        if (mainController != null) {
            mainController.showInvoices();
        }
    }

    @FXML
    private void onViewAllCheques() {
        if (!can(Permission.VIEW_CHEQUES)) {
            showPermissionDenied();
            return;
        }
        if (mainController != null) {
            mainController.showCheques();
        }
    }

    @FXML
    private void onViewAllInvoices() {
        if (!can(Permission.VIEW_INVOICES)) {
            showPermissionDenied();
            return;
        }
        if (mainController != null) {
            mainController.showInvoices();
        }
    }

    private void applyPermissions() {
        boolean canCreateCheque = can(Permission.CREATE_CHEQUE);
        boolean canViewCheques = can(Permission.VIEW_CHEQUES);
        boolean canViewInvoices = can(Permission.VIEW_INVOICES);

        setVisibleManaged(btnNewCheque, canCreateCheque);
        setVisibleManaged(btnQuickNewCheque, canCreateCheque);
        setVisibleManaged(btnViewAllCheques, canViewCheques);
        setVisibleManaged(btnQuickReviewCheques, canViewCheques);
        setVisibleManaged(btnNewInvoice, canViewInvoices);
        setVisibleManaged(btnViewAllInvoices, canViewInvoices);
        setVisibleManaged(btnQuickOpenInvoices, canViewInvoices);
    }

    private boolean can(Permission permission) {
        return AccessControl.can(SessionManager.getInstance().currentUser().orElse(null), permission);
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void showPermissionDenied() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "You do not have permission to perform this action.");
        alert.setTitle("Permission Denied");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void configureRootSizing() {
        if (dashboardRoot != null) {
            dashboardRoot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
    }

    private void configureTables() {
        if (tblRecentCheques != null) {
            setCell(colChequeNo, new PropertyValueFactory<>("chequeNo"));
            setCell(colPayee, new PropertyValueFactory<>("payeeName"));
            setCell(colBank, new PropertyValueFactory<>("bankName"));
            setCell(colAmount, c -> new SimpleStringProperty(formatAmount(c.getValue().getAmount())));
            setCell(colChequeDate, c -> new SimpleStringProperty(formatDate(c.getValue().getIssueDate())));
            setCell(colStatus, c -> new SimpleStringProperty(formatStatus(c.getValue().getStatus())));
            if (colStatus != null) {
                colStatus.setCellFactory(col -> new TableCell<Cheque, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(null);
                        setGraphic(empty || item == null ? null : statusBadge(item));
                        setAlignment(javafx.geometry.Pos.CENTER);
                    }
                });
            }
        }

        if (tblRecentInvoices != null) {
            setCell(colInvoiceNo, new PropertyValueFactory<>("invoiceNo"));
            setCell(colInvoiceClient, new PropertyValueFactory<>("clientName"));
            setCell(colInvoiceAmount, c -> new SimpleStringProperty(formatAmount(c.getValue().getAmount())));
            setCell(colInvoiceDue, c -> new SimpleStringProperty(formatDate(c.getValue().getDueDate())));
            setCell(colInvoiceStatus, c -> new SimpleStringProperty(formatStatus(c.getValue().getStatus())));
            if (colInvoiceStatus != null) {
                colInvoiceStatus.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(null);
                        setGraphic(empty || item == null ? null : invoiceStatusBadge(item));
                    }
                });
            }
        }
    }

    private <S> void setCell(TableColumn<S, String> column, javafx.util.Callback<TableColumn.CellDataFeatures<S, String>, javafx.beans.value.ObservableValue<String>> factory) {
        if (column != null) {
            column.setCellValueFactory(factory);
        }
    }

    private void configureCharts() {
        if (chequeChart != null) {
            chequeChart.setAnimated(false);
            chequeChart.setLegendVisible(false);
            chequeChart.setCreateSymbols(false);
        }
        if (statusChart != null) {
            statusChart.setLabelsVisible(false);
            statusChart.setLegendVisible(false);
        }
    }

    private void configureSearch() {
        if (txtChequeSearch != null) {
            txtChequeSearch.textProperty().addListener((obs, oldValue, newValue) -> applyChequeFilter(newValue));
        }
    }

    private void showEmptyDashboard() {
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, Admin");
        }
        if (lblSubtitle != null) {
            lblSubtitle.setText("Dashboard is ready. Live totals will appear when the database is available.");
        }
        setPlainCount(lblTotalCheques, 0);
        setPlainCount(lblPendingCheques, 0);
        setPlainCount(lblPrintedCheques, 0);
        setPlainCount(lblClearedCount, 0);
        updateChequeChart(Collections.emptyList());
        updateStatusChart(Collections.emptyList());
    }

    private void animateInitialView() {
        FxUtils.animateIn(lblWelcome, 0);
        FxUtils.animateIn(lblSubtitle, 60);
        addHover(cardTotal);
        addHover(cardPending);
        addHover(cardPrinted);
        addHover(cardAmount);
    }

    private DashboardData loadDashboardData() {
        List<Cheque> cheques = Collections.emptyList();
        List<Invoice> invoices = Collections.emptyList();
        User profile = null;
        String error = null;

        try {
            cheques = chequeService.getAll();
        } catch (Exception e) {
            error = appendError(error, "Cheques: " + e.getMessage());
        }

        try {
            invoices = invoiceService.getAll();
        } catch (Exception e) {
            error = appendError(error, "Invoices: " + e.getMessage());
        }

        try {
            profile = userService.loadProfile();
        } catch (Exception e) {
            error = appendError(error, "Profile: " + e.getMessage());
        }

        return new DashboardData(cheques, invoices, profile, error);
    }

    private void applyDashboardData(DashboardData data) {
        loadedCheques = data.cheques() == null ? Collections.emptyList() : data.cheques();
        loadedInvoices = data.invoices() == null ? Collections.emptyList() : data.invoices();

        String displayName = data.profile() != null && data.profile().getName() != null
                && !data.profile().getName().isBlank()
                ? data.profile().getName()
                : "Admin";

        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + displayName);
        }
        if (lblSubtitle != null) {
            lblSubtitle.setText(data.error() == null
                    ? "Your cheque printing operations are running smoothly."
                    : "Dashboard loaded with limited data. " + data.error());
        }

        int total = loadedCheques.size();
        int pending = countCheques(Cheque.Status.Pending);
        int printed = countCheques(Cheque.Status.Printed);
        int printedThisWeek = countPrintedThisWeek();

        setCount(lblTotalCheques, total);
        setCount(lblPendingCheques, pending);
        setCount(lblPrintedCheques, printed);
        setCount(lblClearedCount, printedThisWeek);

        applyChequeFilter(txtChequeSearch == null ? "" : txtChequeSearch.getText());
        if (tblRecentInvoices != null) {
            tblRecentInvoices.getItems().setAll(firstItems(loadedInvoices, 8));
            tblRecentInvoices.refresh();
        }
        updateChequeChart(loadedCheques);
        updateStatusChart(loadedCheques);

        animateCard(cardTotal, 80);
        animateCard(cardPending, 160);
        animateCard(cardPrinted, 240);
        animateCard(cardAmount, 320);
    }

    private void applyChequeFilter(String query) {
        if (tblRecentCheques == null) {
            return;
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Cheque> filtered = loadedCheques.stream()
                .filter(c -> needle.isBlank() || contains(c.getChequeNo(), needle)
                        || contains(c.getPayeeName(), needle)
                        || contains(c.getBankName(), needle)
                        || contains(formatStatus(c.getStatus()), needle))
                .limit(8)
                .toList();
        tblRecentCheques.getItems().setAll(filtered);
        tblRecentCheques.refresh();
    }

    private void updateChequeChart(List<Cheque> cheques) {
        if (chequeChart == null) {
            return;
        }

        LocalDate start = LocalDate.now().minusDays(29);
        Map<LocalDate, Long> countsByDate = cheques.stream()
                .filter(c -> c.getIssueDate() != null && !c.getIssueDate().isBefore(start))
                .collect(Collectors.groupingBy(Cheque::getIssueDate, Collectors.counting()));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            series.getData().add(new XYChart.Data<>(i + 1, countsByDate.getOrDefault(day, 0L)));
        }

        chequeChart.getData().setAll(series);
    }

    private void updateStatusChart(List<Cheque> cheques) {
        if (statusChart == null) {
            return;
        }

        Map<Cheque.Status, Long> counts = cheques.stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.groupingBy(Cheque::getStatus, Collectors.counting()));

        List<PieChart.Data> data = new ArrayList<>();
        addPieSlice(data, "Draft", counts.get(Cheque.Status.Draft));
        addPieSlice(data, "Pending", counts.get(Cheque.Status.Pending));
        addPieSlice(data, "Approved", counts.get(Cheque.Status.Approved));
        addPieSlice(data, "Printed", counts.get(Cheque.Status.Printed));
        addPieSlice(data, "Cancelled", counts.get(Cheque.Status.Cancelled));
        addPieSlice(data, "Deposited", counts.get(Cheque.Status.Deposited));
        addPieSlice(data, "Cleared", counts.get(Cheque.Status.Cleared));
        addPieSlice(data, "Bounced", counts.get(Cheque.Status.Bounced));
        addPieSlice(data, "Rejected", counts.get(Cheque.Status.Rejected));

        // Calculate total count represented on the chart
        long totalCheques = (long) data.stream().mapToDouble(PieChart.Data::getPieValue).sum();
        if (lblDonutTotal != null) {
            lblDonutTotal.setText(String.valueOf(totalCheques));
        }

        if (data.isEmpty()) {
            data.add(new PieChart.Data("No Data", 0));
        }
        statusChart.setData(FXCollections.observableArrayList(data));

        // Clear overlay labels
        if (statusChartOverlay != null) {
            statusChartOverlay.getChildren().clear();
        }

        // Update custom legend
        if (legendContainer != null) {
            legendContainer.getChildren().clear();
        }

        double startAngle = statusChart.getStartAngle();
        boolean clockwise = statusChart.isClockwise();
        double centerX = statusChartOverlay != null ? statusChartOverlay.getWidth() / 2.0 : 110.0;
        double centerY = (statusChartOverlay != null ? statusChartOverlay.getHeight() / 2.0 : 110.0) + 8.0;
        if (centerX == 0) centerX = 110.0;
        if (centerY == 8.0) centerY = 118.0;

        for (PieChart.Data d : statusChart.getData()) {
            String status = d.getName();
            String color = getColorForStatus(status);

            // Set slice color dynamically and add listener for future updates
            javafx.scene.Node sliceNode = d.getNode();
            if (sliceNode != null) {
                sliceNode.setStyle("-fx-pie-color: " + color + "; -fx-border-color: #ffffff; -fx-border-width: 2.5px;");
            }
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + color + "; -fx-border-color: #ffffff; -fx-border-width: 2.5px;");
                }
            });

            // Create custom legend item and draw overlay label if value > 0 and status is not "No Data"
            if (d.getPieValue() > 0 && !"No Data".equals(status)) {
                double pct = totalCheques > 0 ? (d.getPieValue() / (double) totalCheques) * 100.0 : 0.0;
                double arcLength = totalCheques > 0 ? (d.getPieValue() / (double) totalCheques) * 360.0 : 0.0;

                double centerAngle;
                if (clockwise) {
                    centerAngle = startAngle - (arcLength / 2.0);
                    startAngle -= arcLength;
                } else {
                    centerAngle = startAngle + (arcLength / 2.0);
                    startAngle += arcLength;
                }

                // Draw overlay bubble percentage label
                if (statusChartOverlay != null) {
                    double labelRadius = 76.0; // visual center of slices
                    double rad = Math.toRadians(centerAngle);
                    double x = centerX + labelRadius * Math.cos(rad);
                    double y = centerY - labelRadius * Math.sin(rad);

                    String pctStr = pct % 1 == 0 ? String.format(Locale.ROOT, "%.0f%%", pct) : String.format(Locale.ROOT, "%.1f%%", pct);
                    Label label = new Label(pctStr);
                    label.setStyle("-fx-text-fill: #ffffff; " +
                                   "-fx-font-weight: bold; " +
                                   "-fx-font-size: 11.5px; " +
                                   "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.85), 3, 0, 0, 0);");

                    final double fx = x;
                    final double fy = y;
                    label.layoutBoundsProperty().addListener((obs2, oldBounds, newBounds) -> {
                        label.setTranslateX(fx - newBounds.getWidth() / 2.0);
                        label.setTranslateY(fy - newBounds.getHeight() / 2.0);
                    });
                    label.setTranslateX(fx - 18);
                    label.setTranslateY(fy - 8);

                    statusChartOverlay.getChildren().add(label);
                }

                if (legendContainer != null) {
                    legendContainer.getChildren().add(createLegendItem(status, (long) d.getPieValue(), pct, color));
                }
            }
        }
    }

    private void addPieSlice(List<PieChart.Data> data, String label, Long value) {
        if (value != null && value > 0) {
            data.add(new PieChart.Data(label, value));
        }
    }

    private String getColorForStatus(String status) {
        return switch (status) {
            case "Approved" -> "#6366f1";   // Indigo
            case "Pending" -> "#d97706";    // Amber/Orange
            case "Printed" -> "#10b981";    // Emerald Green
            case "Cancelled" -> "#1e293b";  // Navy
            case "Draft" -> "#94a3b8";      // Slate Gray
            case "Deposited" -> "#3b82f6";  // Blue
            case "Cleared" -> "#059669";    // Dark Green
            case "Bounced" -> "#ef4444";    // Red
            case "Rejected" -> "#dc2626";   // Crimson
            default -> "#64748b";
        };
    }

    private String getEmojiForStatus(String status) {
        return switch (status) {
            case "Approved" -> "✔";
            case "Pending" -> "⏳";
            case "Printed" -> "🖨";
            case "Cancelled" -> "✖";
            case "Draft" -> "✎";
            case "Deposited" -> "📥";
            case "Cleared" -> "🏦";
            case "Bounced" -> "⚠️";
            case "Rejected" -> "⛔";
            default -> "●";
        };
    }

    private javafx.scene.Node createLegendItem(String status, long count, double percentage, String color) {
        javafx.scene.layout.HBox item = new javafx.scene.layout.HBox(10);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.getStyleClass().add("status-legend-item");
        item.setPrefWidth(125);

        // Icon Badge (Circle)
        javafx.scene.layout.StackPane badge = new javafx.scene.layout.StackPane();
        badge.setPrefSize(34, 34);
        badge.setMinSize(34, 34);
        badge.setMaxSize(34, 34);
        badge.getStyleClass().add("status-legend-badge");
        badge.setStyle("-fx-background-color: " + color + ";");

        Label iconLabel = new Label(getEmojiForStatus(status));
        iconLabel.getStyleClass().add("status-legend-icon");
        badge.getChildren().add(iconLabel);

        // VBox for labels
        javafx.scene.layout.VBox labels = new javafx.scene.layout.VBox(1);
        labels.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add("status-legend-name");

        String pctStr = percentage % 1 == 0 ? String.format(Locale.ROOT, "%.0f%%", percentage) : String.format(Locale.ROOT, "%.1f%%", percentage);
        Label pctLabel = new Label(pctStr);
        pctLabel.getStyleClass().add("status-legend-pct");

        labels.getChildren().addAll(statusLabel, pctLabel);
        item.getChildren().addAll(badge, labels);

        return item;
    }

    private int countCheques(Cheque.Status status) {
        return (int) loadedCheques.stream().filter(c -> c.getStatus() == status).count();
    }

    private int countPrintedThisWeek() {
        LocalDate start = LocalDate.now().minusDays(6);
        return (int) loadedCheques.stream()
                .filter(c -> c.getStatus() == Cheque.Status.Printed)
                .filter(c -> c.getIssueDate() != null && !c.getIssueDate().isBefore(start))
                .count();
    }

    private <T> List<T> firstItems(List<T> source, int max) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().limit(max).toList();
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String appendError(String current, String next) {
        if (next == null || next.isBlank()) {
            return current;
        }
        return current == null ? next : current + " | " + next;
    }

    private void setCount(Label label, int value) {
        if (label != null) {
            FxUtils.countUp(label, value, "", 100);
        }
    }

    private void setPlainCount(Label label, int value) {
        if (label != null) {
            label.setText(String.valueOf(value));
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

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "" : "\u20B9" + amount.toPlainString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatStatus(Enum<?> status) {
        return status == null ? "Unknown" : status.name();
    }

    private Label statusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("status-badge");
        switch (status) {
            case "Printed" -> badge.getStyleClass().add("status-printed");
            case "Pending" -> badge.getStyleClass().add("status-pending");
            case "Draft" -> badge.getStyleClass().add("status-draft");
            case "Approved" -> badge.getStyleClass().add("status-approved");
            case "Rejected" -> badge.getStyleClass().add("status-rejected");
            case "Cancelled" -> badge.getStyleClass().add("status-cancelled");
            default -> badge.getStyleClass().add("status-neutral");
        }
        return badge;
    }

    private Label invoiceStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("status-badge");
        switch (status) {
            case "Paid" -> badge.getStyleClass().add("status-paid");
            case "Unpaid" -> badge.getStyleClass().add("status-unpaid");
            case "Partial" -> badge.getStyleClass().add("status-partial");
            case "Cancelled" -> badge.getStyleClass().add("status-cancelled");
            default -> badge.getStyleClass().add("status-neutral");
        }
        return badge;
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> reload()));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    public void cleanup() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private record DashboardData(List<Cheque> cheques, List<Invoice> invoices, User profile, String error) {
    }
}
