package com.chequeprint.printpreview;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class PrintPreviewController {

    @FXML
    private Label lblDocTitle;

    @FXML
    private Label lblSize;

    @FXML
    private ComboBox<String> cmbZoom;

    @FXML
    private ComboBox<String> cmbPrinter;

    @FXML
    private WebView previewWebView;

    @FXML
    private Button btnPrint;

    @FXML
    private Button btnSavePdf;

    private PrintPreviewDocument document;
    private boolean printed;
    private boolean contentReady;
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

        WebEngine engine = previewWebView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {

                contentReady = true;

                setButtonsEnabled(true, canSave);

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

        if (document == null) {
            showAlert(
                    "Print",
                    "Unable to print: no document loaded.",
                    Alert.AlertType.ERROR);
            return;
        }

        String printerName = cmbPrinter.getValue();
        Printer printer = selectPrinter(printerName);
        if (printer == null) {
            showAlert(
                    "Print",
                    "No printer is available. Please check printer settings.",
                    Alert.AlertType.ERROR);
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            showAlert(
                    "Printer",
                    "Unable to create a printer job.",
                    Alert.AlertType.ERROR);
            return;
        }

        if (document.getJobName() != null && !document.getJobName().isBlank()) {
            job.getJobSettings().setJobName(document.getJobName());
        }

        PageOrientation orientation = document.getWidthMm() >= document.getHeightMm()
                ? PageOrientation.LANDSCAPE
                : PageOrientation.PORTRAIT;

        Paper paper = choosePaper(printer, orientation);
        PageLayout layout = printer.createPageLayout(paper, orientation, Printer.MarginType.HARDWARE_MINIMUM);
        job.getJobSettings().setPageLayout(layout);

        boolean printedPage = job.printPage(previewWebView);
        if (!printedPage) {
            showAlert(
                    "Print",
                    "Unable to print the preview content.",
                    Alert.AlertType.ERROR);
            return;
        }

        boolean ended = job.endJob();
        if (ended) {
            printed = true;
            closeWindow();
        } else {
            showAlert(
                    "Print",
                    "Print job was cancelled or failed.",
                    Alert.AlertType.INFORMATION);
        }
    }

    // =========================================================
    // SAVE PDF BUTTON
    // =========================================================

    @FXML
    private void onSavePdf() {

        if (document == null || document.getPdfSaveHandler() == null) {

            showAlert(
                    "PDF",
                    "Save as PDF not available.",
                    Alert.AlertType.ERROR);

            return;
        }

        try {

            // Generate temp PDF
            String generatedPdf = document.getPdfSaveHandler().savePdf();

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

            Path sourcePath = Path.of(generatedPdf).toAbsolutePath().normalize();
            if (!Files.exists(sourcePath) || Files.size(sourcePath) == 0) {
                throw new IllegalStateException("Generated PDF is missing or empty.");
            }

            Path targetPath = withPdfExtension(saveFile.toPath()).toAbsolutePath().normalize();
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.copy(
                    sourcePath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING);

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

        if (zoomLabel == null) {

            previewWebView.setZoom(1.0);

            return;
        }

        String numeric = zoomLabel.replace("%", "").trim();

        try {

            double factor = Double.parseDouble(numeric) / 100.0;

            previewWebView.setZoom(factor);

        } catch (Exception ex) {

            previewWebView.setZoom(1.0);
        }
    }

    // =========================================================
    // PRINTER LIST
    // =========================================================

    private void setupPrinters() {

        var printerNames = FXCollections.observableArrayList("Default Printer");
        for (Printer printer : Printer.getAllPrinters()) {
            String name = printer.getName();
            if (name != null && !name.isBlank() && !printerNames.contains(name)) {
                printerNames.add(name);
            }
        }

        cmbPrinter.setItems(printerNames);
        cmbPrinter.setValue("Default Printer");
    }

    // =========================================================
    // BUTTON ENABLE/DISABLE
    // =========================================================

    private Printer selectPrinter(String printerName) {
        if (printerName == null || printerName.isBlank() || printerName.equals("Default Printer")) {
            return Printer.getDefaultPrinter();
        }
        for (Printer printer : Printer.getAllPrinters()) {
            if (printerName.equalsIgnoreCase(printer.getName())) {
                return printer;
            }
        }
        return Printer.getDefaultPrinter();
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

        btnPrint.setDisable(!printEnabled);

        btnSavePdf.setDisable(!saveEnabled);
    }

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
