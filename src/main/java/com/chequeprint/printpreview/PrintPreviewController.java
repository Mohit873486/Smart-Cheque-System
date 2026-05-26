package com.chequeprint.printpreview;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.JobSettings;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
    private ComboBox<Printer> cmbPrinter;

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
        setButtonsEnabled(false);
    }

    public void setDocument(PrintPreviewDocument document) {
        this.document = document;
        this.contentReady = false;
        setButtonsEnabled(false);
        lblDocTitle.setText(document.getDocumentTitle());
        lblSize.setText(String.format(Locale.ROOT, "%.1f mm x %.1f mm", document.getWidthMm(), document.getHeightMm()));

        WebEngine engine = previewWebView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                contentReady = true;
                setButtonsEnabled(true);
            } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                contentReady = false;
                setButtonsEnabled(false);
            }
        });
        engine.loadContent(document.getHtmlContent(), "text/html");
        applyZoom(cmbZoom.getValue());
    }

    public boolean isPrinted() {
        return printed;
    }

    @FXML
    private void onPrint() {
        if (document == null) {
            return;
        }
        if (!contentReady) {
            showAlert("Preview", "Please wait for preview to finish loading.", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            Printer printer = cmbPrinter.getValue() != null ? cmbPrinter.getValue() : Printer.getDefaultPrinter();
            if (printer == null) {
                showAlert("Printer", "No printer found on this system.", Alert.AlertType.WARNING);
                return;
            }
            if (isFaxPrinter(printer)) {
                showAlert("Printer", "Selected printer is Fax. Please choose a real printer or PDF printer.", Alert.AlertType.WARNING);
                return;
            }

            // If user selected a PDF virtual printer, use same save-to-file flow as Save as PDF.
            if (isPdfPrinter(printer)) {
                Path saved = savePdfToUserLocation();
                if (saved != null) {
                    showAlert("PDF Saved", "Saved file:\n" + saved, Alert.AlertType.INFORMATION);
                    printed = true;
                    closeWindow();
                }
                return;
            }

            PrinterJob job = PrinterJob.createPrinterJob(printer);
            if (job == null) {
                showAlert("Printer", "Unable to create print job.", Alert.AlertType.ERROR);
                return;
            }

            JobSettings settings = job.getJobSettings();
            settings.setJobName(document.getJobName());

            PageOrientation orientation = document.getWidthMm() >= document.getHeightMm()
                    ? PageOrientation.LANDSCAPE
                    : PageOrientation.PORTRAIT;

            Paper paper = choosePaper(printer, orientation);
            PageLayout layout = printer.createPageLayout(paper, orientation, Printer.MarginType.HARDWARE_MINIMUM);
            settings.setPageLayout(layout);

            previewWebView.getEngine().print(job);
            boolean ended = job.endJob();
            if (ended) {
                printed = true;
                closeWindow();
            } else {
                showAlert("Print", "Print job was cancelled or failed.", Alert.AlertType.INFORMATION);
            }
        } catch (Exception ex) {
            showAlert("Print Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onSavePdf() {
        if (document == null || document.getPdfSaveHandler() == null) {
            showAlert("PDF", "Save as PDF is not available for this document.", Alert.AlertType.INFORMATION);
            return;
        }
        if (!contentReady) {
            showAlert("Preview", "Please wait for preview to finish loading.", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            Path saved = savePdfToUserLocation();
            if (saved == null) {
                return;
            }
            showAlert("PDF Saved", "Saved file:\n" + saved, Alert.AlertType.INFORMATION);
            openContainingFolder(saved);
        } catch (Exception ex) {
            showAlert("PDF Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private Path savePdfToUserLocation() throws Exception {
        if (document == null || document.getPdfSaveHandler() == null) {
            return null;
        }

        // First generate the PDF to a temporary location so we can validate it.
        String generated = document.getPdfSaveHandler().savePdf();
        Path sourcePath = Path.of(generated).toAbsolutePath().normalize();

        if (!Files.exists(sourcePath)) {
            throw new IllegalStateException("PDF was not generated: " + sourcePath);
        }
        if (Files.size(sourcePath) <= 0) {
            throw new IllegalStateException("Generated PDF is empty: " + sourcePath);
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(suggestPdfName(document.getJobName()));
        if (lastSaveDirectory != null && Files.isDirectory(lastSaveDirectory)) {
            chooser.setInitialDirectory(lastSaveDirectory.toFile());
        }

        Stage stage = (Stage) previewWebView.getScene().getWindow();
        File target = chooser.showSaveDialog(stage);
        if (target == null) {
            return null;
        }

        Path targetPath = withPdfExtension(target.toPath()).toAbsolutePath().normalize();
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        if (!sourcePath.equals(targetPath)) {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        if (!Files.exists(targetPath)) {
            throw new IllegalStateException("PDF save failed at: " + targetPath);
        }
        if (Files.size(targetPath) <= 0) {
            throw new IllegalStateException("Saved PDF is empty: " + targetPath);
        }

        lastSaveDirectory = targetPath.getParent();
        return targetPath;
    }

    @FXML
    private void onClose() {
        closeWindow();
    }

    private void setupZoom() {
        cmbZoom.setItems(FXCollections.observableArrayList("75%", "100%", "125%", "150%"));
        cmbZoom.setValue("100%");
        cmbZoom.valueProperty().addListener((obs, oldVal, newVal) -> applyZoom(newVal));
    }

    private void setupPrinters() {
        cmbPrinter.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Printer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        cmbPrinter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Printer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select printer" : item.getName());
            }
        });

        cmbPrinter.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter != null) {
            cmbPrinter.setValue(defaultPrinter);
        } else if (!cmbPrinter.getItems().isEmpty()) {
            cmbPrinter.setValue(cmbPrinter.getItems().get(0));
        }
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
        } catch (NumberFormatException ex) {
            previewWebView.setZoom(1.0);
        }
    }

    private Paper choosePaper(Printer printer, PageOrientation orientation) {
        Paper target = orientation == PageOrientation.LANDSCAPE ? Paper.A4 : Paper.A4;
        if (printer.getPrinterAttributes().getSupportedPapers().contains(target)) {
            return target;
        }
        return printer.getPrinterAttributes().getDefaultPaper();
    }

    private void closeWindow() {
        Stage stage = (Stage) previewWebView.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String suggestPdfName(String rawName) {
        String base = (rawName == null || rawName.isBlank()) ? "document" : rawName;
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
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return path;
        }
        Path parent = path.getParent();
        String updated = name.isBlank() ? "document.pdf" : name + ".pdf";
        return parent == null ? Path.of(updated) : parent.resolve(updated);
    }

    private boolean isPdfPrinter(Printer printer) {
        if (printer == null || printer.getName() == null) {
            return false;
        }
        String name = printer.getName().toLowerCase(Locale.ROOT);
        return name.contains("pdf");
    }

    private boolean isFaxPrinter(Printer printer) {
        if (printer == null || printer.getName() == null) {
            return false;
        }
        String name = printer.getName().toLowerCase(Locale.ROOT);
        return name.contains("fax");
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnPrint != null) {
            btnPrint.setDisable(!enabled);
        }
        if (btnSavePdf != null) {
            btnSavePdf.setDisable(!enabled);
        }
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
            // Non-blocking helper, ignore failures.
        }
    }
}
