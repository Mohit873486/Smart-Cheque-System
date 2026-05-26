package com.chequeprint.printpreview;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.util.BankTemplateLayoutStore;
import com.chequeprint.util.ChequeSizeCodec;
import com.chequeprint.util.JasperPrintUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PrintPreviewService {

    private final PrintHtmlTemplateService htmlTemplateService = new PrintHtmlTemplateService();
    private final BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();

    public boolean previewCheque(Cheque cheque, Bank bankTemplate) throws Exception {
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque must not be null.");
        }

        BankTemplateLayout layout = resolveLayout(bankTemplate);
        String html = htmlTemplateService.buildChequeHtml(cheque, bankTemplate, layout);

        double widthMm = layout.getWidthInches() * 25.4;
        double heightMm = layout.getHeightInches() * 25.4;

        PrintPreviewDocument doc = new PrintPreviewDocument(
                "Cheque Print Preview",
                "Cheque Preview",
                "Cheque-" + safe(cheque.getChequeNo(), "Draft"),
                html,
                widthMm,
                heightMm,
                () -> JasperPrintUtil.exportChequePdf(cheque, resolveDefaultExportDir().toString(), bankTemplate));

        return showPreview(doc);
    }

    public boolean previewInvoice(Invoice invoice) throws Exception {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice must not be null.");
        }

        String html = htmlTemplateService.buildInvoiceHtml(invoice);

        PrintPreviewDocument doc = new PrintPreviewDocument(
                "Invoice Print Preview",
                "Invoice Preview",
                "Invoice-" + safe(invoice.getInvoiceNo(), "Draft"),
                html,
                210.0,
                297.0,
                () -> JasperPrintUtil.exportInvoicePdf(invoice, resolveDefaultExportDir().toString()));

        return showPreview(doc);
    }

    private boolean showPreview(PrintPreviewDocument document) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return showPreviewInternal(document);
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                result.set(showPreviewInternal(document));
            } catch (Exception ex) {
                errorRef.set(ex);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        return result.get();
    }

    private boolean showPreviewInternal(PrintPreviewDocument document) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/print_preview.fxml"));
        Parent root = loader.load();

        PrintPreviewController controller = loader.getController();
        controller.setDocument(document);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(document.getWindowTitle());
        Scene scene = new Scene(root, 1220, 760);

        String appCss = getClass().getResource("/css/style.css") != null
                ? getClass().getResource("/css/style.css").toExternalForm()
                : null;
        if (appCss != null) {
            scene.getStylesheets().add(appCss);
        }

        String previewCss = getClass().getResource("/css/print-preview.css") != null
                ? getClass().getResource("/css/print-preview.css").toExternalForm()
                : null;
        if (previewCss != null) {
            scene.getStylesheets().add(previewCss);
        }

        stage.setScene(scene);
        stage.showAndWait();

        return controller.isPrinted();
    }

    private BankTemplateLayout resolveLayout(Bank bankTemplate) {
        BankTemplateLayout fromSize = ChequeSizeCodec.decodeLayout(bankTemplate != null ? bankTemplate.getChequeSize() : null);
        if (bankTemplate == null || bankTemplate.getBankCode() == null || bankTemplate.getBankCode().isBlank()) {
            fromSize.ensureAllFields();
            return fromSize;
        }

        String code = bankTemplate.getBankCode().trim().toUpperCase();
        Map<String, BankTemplateLayout> all = layoutStore.loadAll();
        BankTemplateLayout stored = all.get(code);
        if (stored == null) {
            fromSize.ensureAllFields();
            return fromSize;
        }

        BankTemplateLayout merged = stored.copy();
        merged.setWidthInches(fromSize.getWidthInches());
        merged.setHeightInches(fromSize.getHeightInches());
        merged.ensureAllFields();
        return merged;
    }

    private Path resolveDefaultExportDir() {
        Path home = Path.of(System.getProperty("user.home"));
        Path downloads = home.resolve("Downloads");
        Path desktop = home.resolve("Desktop");

        if (Files.exists(downloads)) {
            return downloads;
        }
        if (Files.exists(desktop)) {
            return desktop;
        }
        return home;
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
