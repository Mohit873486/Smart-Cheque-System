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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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

    private PrintPreviewDocument document;
    private boolean printed;

    @FXML
    public void initialize() {
        setupZoom();
        setupPrinters();
        previewWebView.setContextMenuEnabled(false);
    }

    public void setDocument(PrintPreviewDocument document) {
        this.document = document;
        lblDocTitle.setText(document.getDocumentTitle());
        lblSize.setText(String.format(Locale.ROOT, "%.1f mm x %.1f mm", document.getWidthMm(), document.getHeightMm()));

        WebEngine engine = previewWebView.getEngine();
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

        try {
            Printer printer = cmbPrinter.getValue() != null ? cmbPrinter.getValue() : Printer.getDefaultPrinter();
            if (printer == null) {
                showAlert("Printer", "No printer found on this system.", Alert.AlertType.WARNING);
                return;
            }

            // If user selected a PDF virtual printer, use same save-to-file flow as Save as PDF.
            if (isPdfPrinter(printer)) {
                boolean saved = savePdfToUserLocation();
                if (saved) {
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

        try {
            boolean saved = savePdfToUserLocation();
            if (!saved) {
                return;
            }
        } catch (Exception ex) {
            showAlert("PDF Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean savePdfToUserLocation() throws Exception {
        if (document == null || document.getPdfSaveHandler() == null) {
            return false;
        }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName(suggestPdfName(document.getJobName()));

            Stage stage = (Stage) previewWebView.getScene().getWindow();
            File target = chooser.showSaveDialog(stage);
            if (target == null) {
                return false;
            }

            Path targetPath = withPdfExtension(target.toPath()).toAbsolutePath().normalize();
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            String generated = document.getPdfSaveHandler().savePdf();
            Path sourcePath = Path.of(generated).toAbsolutePath().normalize();

            if (!Files.exists(sourcePath)) {
                throw new IllegalStateException("PDF was not generated: " + sourcePath);
            }

            if (!sourcePath.equals(targetPath)) {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.exists(targetPath)) {
                throw new IllegalStateException("PDF save failed at: " + targetPath);
            }

            return true;
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
}
