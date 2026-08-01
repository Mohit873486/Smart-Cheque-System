package com.chequeprint.printpreview;

import com.chequeprint.util.AppState;
import com.chequeprint.util.PrinterUtils;
import com.chequeprint.service.FxPrinterService;
import com.chequeprint.service.PrintService;
import com.chequeprint.service.PrinterErrorHandler;
import com.chequeprint.service.PrinterSelectionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

public class PrintPreviewController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label lblDocTitle;

    @FXML
    private Label lblSize;

    @FXML
    private ComboBox<String> cmbZoom;

    @FXML
    private ComboBox<Printer> cmbPrinter;

    @FXML
    private WebView previewWebView;

    @FXML
    private Button btnPrint;

    @FXML
    private Button btnSavePdf;

    @FXML
    private Button btnSetDefaultPrinter;

    @FXML
    private Button btnTestPrint;

    @FXML
    private Button btnClose;

    @FXML
    private VBox controlsPane;

    private PrintPreviewDocument document;
    private final PrintService printService = new PrintService();
    private final PrinterSelectionService printerSelectionService = new PrinterSelectionService();
    private boolean printed;
    private boolean contentReady;
    private boolean printersAvailable;
    private boolean printing;
    private double basePreviewWidthPx;
    private double basePreviewHeightPx;
    private static Path lastSaveDirectory;

    @FXML
    public void initialize() {

        setupZoom();

        setupPrinters();

        previewWebView.setContextMenuEnabled(false);

        setButtonsEnabled(false, false);
    }

    public void setDocument(PrintPreviewDocument document) {

        this.document = document;

        boolean canSave = document.getPdfSaveHandler() != null;

        lblDocTitle.setText(document.getDocumentTitle());

        lblSize.setText(String.format(
                Locale.ROOT,
                "%.1f mm x %.1f mm",
                document.getWidthMm(),
                document.getHeightMm()));

        basePreviewWidthPx = mmToPx(document.getWidthMm());
        basePreviewHeightPx = mmToPx(document.getHeightMm());
        resizePreviewWebView(1.0);

        WebEngine engine = previewWebView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {

                contentReady = true;

                setButtonsEnabled(printersAvailable && cmbPrinter != null && cmbPrinter.getValue() != null, canSave);

            } else if (newState == Worker.State.FAILED) {

                contentReady = false;

                setButtonsEnabled(false, canSave);
            }
        });

        engine.loadContent(document.getHtmlContent(), "text/html");

        applyZoom(cmbZoom.getValue());
    }

    public boolean isPrinted() {
        return printed;
    }

    // =========================================================
    // PRINT BUTTON
    // =========================================================

    @FXML
    private void onPrint() {
        if (printing) {
            return;
        }

        if (document == null) {
            showAlert(
                    "Print",
                    "Unable to print: no preview document loaded.",
                    Alert.AlertType.ERROR);
            return;
        }

        if (!contentReady) {
            showAlert("Print", "Preview is still rendering. Please try again in a moment.", Alert.AlertType.WARNING);
            return;
        }

        Printer printer = cmbPrinter != null ? cmbPrinter.getValue() : null;
        String attemptedPrinterName = null;
        try {
            if (printer == null) {
                printer = printerSelectionService.resolveSelectedOrDefaultPrinter();
                if (printer != null && cmbPrinter != null) {
                    cmbPrinter.setValue(printer);
                }
            }
            if (printer == null) {
                showAlert(
                        "Printer Required",
                        PrinterErrorHandler.buildUserMessage(null, "select", null),
                        Alert.AlertType.WARNING);
                return;
            }
            attemptedPrinterName = printer.getName();
            printerSelectionService.selectPrinter(printer);
            setPrinting(true);
            printRenderedPreviewNode(printer, attemptedPrinterName);
        } catch (Exception ex) {
            setPrinting(false);
            showAlert(
                    "Print Error",
                    PrinterErrorHandler.buildUserMessage(printer, "print", ex),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onSetDefaultPrinter() {
        if (printing) {
            return;
        }

        Printer selectedPrinter = cmbPrinter != null ? cmbPrinter.getValue() : null;
        if (selectedPrinter == null) {
            showAlert("Printer Selection", "Please select a printer from the dropdown list first.", Alert.AlertType.WARNING);
            return;
        }

        try {
            printerSelectionService.setDefaultPrinter(selectedPrinter);
            showAlert("Default Printer Set", "Default printer updated to '" + selectedPrinter.getName() + "' successfully.\nThis choice will be remembered permanently.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error Setting Printer", "Failed to save default printer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onTestPrint() {
        if (printing) {
            return;
        }

        Printer printer = cmbPrinter != null ? cmbPrinter.getValue() : null;
        if (printer == null) {
            showAlert("Test Print", "Please select a printer to run a test print.", Alert.AlertType.WARNING);
            return;
        }

        try {
            boolean ok = printService.testPrint(previewWebView != null && previewWebView.getScene() != null ? previewWebView.getScene().getWindow() : null, printer);
            if (ok) {
                showAlert("Test Print Success", "Test page submitted successfully to printer: " + printer.getName(), Alert.AlertType.INFORMATION);
            } else {
                showAlert("Test Print Failed", "Test print job failed or was cancelled on printer: " + printer.getName(), Alert.AlertType.WARNING);
            }
        } catch (Exception ex) {
            showAlert("Test Print Error", "Test print failed on printer '" + printer.getName() + "':\n" + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =========================================================
    // SAVE PDF BUTTON
    // =========================================================

    @FXML
    private void onSavePdf() {
        if (printing) {
            return;
        }

        if (document == null) {

            showAlert(
                    "PDF",
                    "Save as PDF not available.",
                    Alert.AlertType.ERROR);

            return;
        }

        try {
            FileChooser chooser = new FileChooser();

            chooser.setTitle("Save PDF");

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "PDF Files",
                            "*.pdf"));

            chooser.setInitialFileName(suggestPdfName(document.getJobName()));
            if (lastSaveDirectory != null && Files.isDirectory(lastSaveDirectory)) {
                chooser.setInitialDirectory(lastSaveDirectory.toFile());
            }

            File saveFile = chooser.showSaveDialog(
                    previewWebView.getScene().getWindow());

            if (saveFile == null) {
                return;
            }

            Path targetPath = withPdfExtension(saveFile.toPath()).toAbsolutePath().normalize();
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            String tempPdfPath = document.getPdfSaveHandler().savePdf();
            if (tempPdfPath != null && !tempPdfPath.isBlank()) {
                Path tempPath = Path.of(tempPdfPath);
                if (Files.exists(tempPath)) {
                    Files.copy(tempPath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tempPath);
                } else {
                    throw new Exception("Temp PDF file was not generated.");
                }
            } else {
                throw new Exception("PDF export handler returned empty path.");
            }

            lastSaveDirectory = targetPath.getParent();

            showAlert(
                    "PDF Saved",
                    "Saved Successfully:\n" + targetPath.toString(),
                    Alert.AlertType.INFORMATION);
            openContainingFolder(targetPath);

        } catch (Exception ex) {

            ex.printStackTrace();

            showAlert(
                    "PDF Error",
                    ex.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // =========================================================
    // CLOSE BUTTON
    // =========================================================

    @FXML
    private void onClose() {
        if (printing) {
            return;
        }

        Stage stage = (Stage) previewWebView.getScene().getWindow();

        stage.close();
    }

    // =========================================================
    // ZOOM
    // =========================================================

    private void setupZoom() {

        cmbZoom.setItems(
                FXCollections.observableArrayList(
                        "75%",
                        "100%",
                        "125%",
                        "150%"));

        cmbZoom.setValue("100%");

        cmbZoom.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyZoom(newVal);
        });
    }

    private void applyZoom(String zoomLabel) {
        if (printing) {
            return;
        }

        if (zoomLabel == null) {

            previewWebView.setZoom(1.0);

            return;
        }

        String numeric = zoomLabel.replace("%", "").trim();

        try {

            double factor = Double.parseDouble(numeric) / 100.0;

            previewWebView.setZoom(factor);
            resizePreviewWebView(factor);

        } catch (Exception ex) {

            previewWebView.setZoom(1.0);
            resizePreviewWebView(1.0);
        }
    }

    // =========================================================
    // PRINTER LIST
    // =========================================================

    private void setupPrinters() {
        printerSelectionService.refreshAvailablePrinters();
        var printers = FXCollections.observableArrayList(AppState.getInstance().getAvailablePrinters());
        cmbPrinter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Printer printer) {
                return printer != null ? printer.getName() : "";
            }

            @Override
            public Printer fromString(String string) {
                return PrinterUtils.findPrinterByName(string);
            }
        });
        cmbPrinter.setItems(printers);

        if (printers.isEmpty()) {
            printersAvailable = false;
            btnPrint.setDisable(true);
            btnTestPrint.setDisable(true);
            btnSetDefaultPrinter.setDisable(true);
            showAlert("Printers Not Found", "No printers are installed or visible to JavaFX.", Alert.AlertType.WARNING);
            return;
        }
        printersAvailable = true;

        Printer activePrinter = printerSelectionService.initializeDefaultPrinter();
        if (activePrinter != null) {
            for (Printer printer : printers) {
                if (printer.getName().equalsIgnoreCase(activePrinter.getName())) {
                    cmbPrinter.setValue(printer);
                    break;
                }
            }
        }
        setButtonsEnabled(contentReady && cmbPrinter.getValue() != null, document != null && document.getPdfSaveHandler() != null);

        cmbPrinter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!printing && newVal != null) {
                printerSelectionService.selectPrinter(newVal);
            }
            setButtonsEnabled(contentReady && newVal != null, document != null && document.getPdfSaveHandler() != null);
        });
    }

    // =========================================================
    // BUTTON ENABLE/DISABLE
    // =========================================================

    private void printRenderedPreviewNode(Printer printer, String attemptedPrinterName) {
        runAfterPreviewRenderPulse(() -> {
            boolean ok = false;
            boolean errorShown = false;
            try {
                ok = FxPrinterService.printCheque(previewWebView, printer);
            } catch (RuntimeException ex) {
                errorShown = true;
                showAlert(
                        "Print Error",
                        PrinterErrorHandler.buildUserMessage(printer, "print", ex, true),
                        Alert.AlertType.ERROR);
            } finally {
                setPrinting(false);
            }

            if (ok) {
                printed = true;
                closeWindow();
            } else if (!errorShown) {
                showAlert(
                        "Print",
                        PrinterErrorHandler.buildUserMessage(printer, "print", null, true),
                        Alert.AlertType.WARNING);
            }
        });
    }

    private void runAfterPreviewRenderPulse(Runnable action) {
        previewWebView.applyCss();
        if (previewWebView.getParent() != null) {
            previewWebView.getParent().applyCss();
            previewWebView.getParent().layout();
        }
        previewWebView.layout();

        Platform.runLater(() -> {
            previewWebView.applyCss();
            previewWebView.layout();
            Platform.runLater(action);
        });
    }

    private void setPrinting(boolean printing) {
        this.printing = printing;
        if (controlsPane != null) {
            controlsPane.setDisable(printing);
        }
        if (btnPrint != null) {
            btnPrint.setDisable(printing || !printersAvailable || !contentReady);
            btnPrint.setText(printing ? "Printing..." : "Print");
        }
        if (btnSavePdf != null) {
            btnSavePdf.setDisable(printing || document == null || document.getPdfSaveHandler() == null);
        }
        if (btnClose != null) {
            btnClose.setDisable(printing);
        }
        if (previewWebView != null) {
            previewWebView.setMouseTransparent(printing);
        }
    }

    private Paper choosePaper(Printer printer, PageOrientation orientation) {
        Paper target = Paper.A4;
        if (printer != null) {
            if (printer.getPrinterAttributes().getSupportedPapers().contains(target)) {
                return target;
            }
            return printer.getPrinterAttributes().getDefaultPaper();
        }
        return target;
    }

    private void closeWindow() {
        Stage stage = (Stage) previewWebView.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void setButtonsEnabled(
            boolean printEnabled,
            boolean saveEnabled) {

        btnPrint.setDisable(printing || !printEnabled);

        btnSavePdf.setDisable(printing || !saveEnabled);
    }

    private void resizePreviewWebView(double zoomFactor) {
        if (basePreviewWidthPx <= 0 || basePreviewHeightPx <= 0) {
            return;
        }

        double width = basePreviewWidthPx * zoomFactor;
        double height = basePreviewHeightPx * zoomFactor;

        previewWebView.setPrefSize(width, height);
        previewWebView.setMinSize(width, height);
        previewWebView.setMaxSize(width, height);
    }

    private double mmToPx(double mm) {
        return mm * 96.0 / 25.4;
    }

    private double mmToPt(double mm) {
        return mm * 72.0 / 25.4;
    }

    // WebView snapshot PDF exporter removed to use JasperReports exclusively

    private void openContainingFolder(Path file) {
        try {
            if (file == null || file.getParent() == null) {
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.getParent().toFile());
            }
        } catch (Exception ignored) {
            // ignore non-critical folder open errors
        }
    }

    private String suggestPdfName(String rawName) {
        String base = rawName == null || rawName.isBlank() ? "document" : rawName;
        String clean = base.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        if (clean.isEmpty()) {
            clean = "document";
        }
        if (!clean.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            clean = clean + ".pdf";
        }
        return clean;
    }

    private Path withPdfExtension(Path path) {
        String fileName = path.getFileName() != null ? path.getFileName().toString() : "document.pdf";
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return path;
        }
        Path parent = path.getParent();
        return parent == null ? Path.of(fileName + ".pdf") : parent.resolve(fileName + ".pdf");
    }

    // =========================================================
    // ALERT
    // =========================================================

    private void showAlert(
            String title,
            String msg,
            Alert.AlertType type) {

        Alert alert = new Alert(type);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(msg);

        alert.showAndWait();
    }
}
