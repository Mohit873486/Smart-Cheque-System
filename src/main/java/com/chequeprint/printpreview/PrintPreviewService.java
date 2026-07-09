package com.chequeprint.printpreview;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.util.BankTemplateLayoutStore;
import com.chequeprint.util.ChequeSizeCodec;
import com.chequeprint.util.JasperPrintUtil;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

public class PrintPreviewService {

    private final PrintHtmlTemplateService htmlTemplateService = new PrintHtmlTemplateService();
    private final BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();

    public boolean previewCheque(Cheque cheque, Bank bankTemplate) throws Exception {
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque must not be null.");
        }

        BankTemplateLayout layout = resolveLayout(bankTemplate);
        double widthMm = layout.getWidthInches() * 25.4;
        double heightMm = layout.getHeightInches() * 25.4;

        // Compile and fill the actual JasperReport matching dynamic template.json positions
        JasperPrint jasperPrint = JasperPrintUtil.generateChequePrintObject(cheque, bankTemplate);
        // Render first page to AWT Image and convert to BufferedImage
        java.awt.Image pageImage = JasperPrintManager.printPageToImage(jasperPrint, 0, 2.0f);
        BufferedImage img;
        if (pageImage instanceof BufferedImage) {
            img = (BufferedImage) pageImage;
        } else {
            img = new BufferedImage(
                    pageImage.getWidth(null),
                    pageImage.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = img.createGraphics();
            g2d.drawImage(pageImage, 0, 0, null);
            g2d.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

        String html = "<html><body style='margin:0;padding:0;background:#f8fafc;display:flex;justify-content:center;align-items:center;height:100vh;'>" +
                "<img src='data:image/png;base64," + base64Image + "' style='max-width:100%;max-height:100%;box-shadow:0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05);border:1px solid #e2e8f0;background:white;' />" +
                "</body></html>";

        PrintPreviewDocument doc = new PrintPreviewDocument(
                "Cheque Print Preview",
                "Cheque Preview",
                "Cheque-" + safe(cheque.getChequeNo(), "Draft"),
                html,
                widthMm,
                heightMm,
                (printer) -> JasperPrintUtil.printCheque(cheque, bankTemplate, printer),
                () -> JasperPrintUtil.exportChequePdf(cheque, createTempExportDir().toString(), bankTemplate));

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
                (printer) -> JasperPrintUtil.printInvoice(invoice, printer),
                () -> JasperPrintUtil.exportInvoicePdf(invoice, createTempExportDir().toString()));

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

    private Path createTempExportDir() throws Exception {
        return Files.createTempDirectory("chequepro-preview-");
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
