package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.printpreview.PrintPreviewService;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.util.ChequeSnapshotRenderer;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JasperPrintUtil {

    private static final String DEFAULT_CHEQUE_TEMPLATE = "/reports/cheque_report.jrxml";
    private static final PrintPreviewService PREVIEW_SERVICE = new PrintPreviewService();
    private static volatile String lastUsedPrinterName = "Default Printer";

    public static String getLastUsedPrinterName() {
        return lastUsedPrinterName;
    }

    public static void setLastUsedPrinterName(String printerName) {
        lastUsedPrinterName = printerName != null ? printerName : "Default Printer";
    }

    public static boolean printCheque(Cheque cheque) throws JRException {
        return printCheque(cheque, null);
    }

    public static boolean printCheque(Cheque cheque, Bank bankTemplate) throws JRException {
        return printCheque(cheque, bankTemplate, javafx.print.Printer.getDefaultPrinter());
    }

    public static boolean printCheque(Cheque cheque, Bank bankTemplate, javafx.print.Printer printer) throws JRException {
        setLastUsedPrinterName(printer != null ? printer.getName() : "Default Printer");
        try {
            BankTemplateLayout layout = ChequeSnapshotRenderer.resolveLayout(bankTemplate);
            WritableImage snapshot = ChequeSnapshotRenderer.renderCheque(cheque, bankTemplate, layout, 3.0);
            boolean success = ChequeSnapshotRenderer.printSnapshot(snapshot, printer, "Cheque-" + nvl(cheque.getChequeNo(), "Draft"), layout);
            if (success) {
                cheque.setStatus(Cheque.Status.Printed);
            }
            return success;
        } catch (Exception e) {
            throw new JRException("Print failed: " + e.getMessage(), e);
        }
    }

    public static boolean previewCheque(Cheque cheque, Bank bankTemplate) throws JRException {
        try {
            boolean printed = PREVIEW_SERVICE.previewCheque(cheque, bankTemplate);
            if (printed) {
                cheque.setStatus(Cheque.Status.Printed);
            }
            return printed;
        } catch (JRException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JRException("Unable to open cheque preview.", ex);
        }
    }

    public static boolean previewInvoice(Invoice invoice) throws JRException {
        try {
            return PREVIEW_SERVICE.previewInvoice(invoice);
        } catch (JRException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JRException("Unable to open invoice preview.", ex);
        }
    }

    public static boolean printInvoice(Invoice invoice) throws JRException {
        return printInvoice(invoice, javafx.print.Printer.getDefaultPrinter());
    }

    public static boolean printInvoice(Invoice invoice, javafx.print.Printer printer) throws JRException {
        setLastUsedPrinterName(printer != null ? printer.getName() : "Default Printer");
        InputStream template = JasperPrintUtil.class
                .getResourceAsStream("/reports/invoice_report.jrxml");

        if (template == null) {
            throw new JRException(
                    "invoice_report.jrxml not found in /reports/. Add the file to src/main/resources/reports/");
        }

        JasperReport jr = JasperCompileManager.compileReport(template);
        Map<String, Object> params = buildInvoiceParams(invoice);
        JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource());
        return printJasperPrintWithJavaFX(print, printer);
    }

    private static boolean printJasperPrintWithJavaFX(JasperPrint print, javafx.print.Printer printer) throws JRException {
        try {
            java.awt.Image pageImage = JasperPrintManager.printPageToImage(print, 0, 2.0f);
            BufferedImage bufferedImage;
            if (pageImage instanceof BufferedImage) {
                bufferedImage = (BufferedImage) pageImage;
            } else {
                bufferedImage = new BufferedImage(
                        pageImage.getWidth(null),
                        pageImage.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g2d = bufferedImage.createGraphics();
                g2d.drawImage(pageImage, 0, 0, null);
                g2d.dispose();
            }

            javafx.scene.image.WritableImage fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
            imageView.setFitWidth(print.getPageWidth());
            imageView.setFitHeight(print.getPageHeight());
            imageView.setPreserveRatio(true);

            javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(imageView);

            if (Platform.isFxApplicationThread()) {
                return executePrinterJob(pane, printer);
            } else {
                java.util.concurrent.FutureTask<Boolean> task = new java.util.concurrent.FutureTask<>(() -> {
                    return executePrinterJob(pane, printer);
                });
                Platform.runLater(task);
                return task.get();
            }
        } catch (Exception e) {
            throw new JRException("JavaFX print job failed: " + e.getMessage(), e);
        }
    }

    private static boolean executePrinterJob(javafx.scene.Node node, javafx.print.Printer printer) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            return false;
        }
        if (printer != null) {
            job.setPrinter(printer);
        }
        boolean success = job.printPage(node);
        if (success) {
            return job.endJob();
        }
        return false;
    }

    public static String exportChequePdf(Cheque cheque, String outputDir) throws JRException {
        return exportChequePdf(cheque, outputDir, null);
    }

    public static String exportChequePdf(Cheque cheque, String outputDir, Bank bankTemplate) throws JRException {
        try {
            BankTemplateLayout layout = ChequeSnapshotRenderer.resolveLayout(bankTemplate);
            WritableImage snapshot = ChequeSnapshotRenderer.renderCheque(cheque, bankTemplate, layout, 3.0);

            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String pdfPath = outputDir + File.separator + nvl(cheque.getChequeNo(), "cheque") + ".pdf";

            double widthPoints = layout.getWidthInches() * 72.0;
            double heightPoints = layout.getHeightInches() * 72.0;

            Document document = new Document(new Rectangle((float) widthPoints, (float) heightPoints), 0, 0, 0, 0);
            FileOutputStream fos = new FileOutputStream(pdfPath);
            PdfWriter.getInstance(document, fos);
            document.open();

            BufferedImage bufImg = SwingFXUtils.fromFXImage(snapshot, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufImg, "png", baos);

            com.lowagie.text.Image pdfImg = com.lowagie.text.Image.getInstance(baos.toByteArray());
            pdfImg.scaleAbsolute((float) widthPoints, (float) heightPoints);
            document.add(pdfImg);

            document.close();
            fos.close();

            return pdfPath;
        } catch (Exception e) {
            throw new JRException("PDF export failed: " + e.getMessage(), e);
        }
    }

    public static String exportInvoicePdf(Invoice invoice, String outputDir) throws JRException {

        InputStream template = JasperPrintUtil.class
                .getResourceAsStream("/reports/invoice_report.jrxml");

        if (template == null) {
            throw new JRException(
                    "invoice_report.jrxml not found in /reports/. Add the file to src/main/resources/reports/");
        }

        JasperReport jr = JasperCompileManager.compileReport(template);

        Map<String, Object> params = buildInvoiceParams(invoice);
        JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource());

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String pdfPath = outputDir + File.separator + nvl(invoice.getInvoiceNo(), "invoice") + ".pdf";

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfPath));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataTitle("Invoice - " + nvl(invoice.getInvoiceNo(), ""));
        config.setMetadataAuthor("ChequePro v2.0");
        exporter.setConfiguration(config);
        exporter.exportReport();

        return pdfPath;
    }

    public static JasperPrint generateChequePrintObject(Cheque cheque, Bank bankTemplate) throws JRException {
        JasperReport jr = compileChequeReport(cheque, bankTemplate);
        Map<String, Object> params = buildChequeParams(cheque);
        return JasperFillManager.fillReport(jr, params, new JREmptyDataSource());
    }

    private static JasperReport compileChequeReport(Cheque cheque, Bank bankTemplate) throws JRException {
        List<String> candidates = resolveChequeTemplateCandidates(cheque, bankTemplate);

        for (String candidate : candidates) {
            if (candidate.startsWith("file:")) {
                String path = candidate.substring("file:".length());
                File file = new File(path);
                if (file.isFile()) {
                    try {
                        JasperDesign design = JRXmlLoader.load(path);
                        applyTemplatePositions(design);
                        return JasperCompileManager.compileReport(design);
                    } catch (Exception ex) {
                        throw new JRException("Failed to compile file template: " + path, ex);
                    }
                }
                continue;
            }

            try (InputStream template = JasperPrintUtil.class.getResourceAsStream(candidate)) {
                if (template != null) {
                    JasperDesign design = JRXmlLoader.load(template);
                    applyTemplatePositions(design);
                    return JasperCompileManager.compileReport(design);
                }
            } catch (Exception ex) {
                throw new JRException("Failed to compile cheque template: " + candidate, ex);
            }
        }

        throw new JRException("No cheque template found for bank '"
                + nvl(cheque.getBankName(), "Unknown")
                + "'. Tried: " + String.join(", ", candidates));
    }

    private static Map<String, Map<String, Integer>> loadTemplatePositions() {
        try (InputStream is = JasperPrintUtil.class.getResourceAsStream("/config/template.json")) {
            if (is == null) {
                return new HashMap<>();
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, new TypeReference<Map<String, Map<String, Integer>>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private static void applyTemplatePositions(JasperDesign design) {
        Map<String, Map<String, Integer>> positions = loadTemplatePositions();
        if (positions.isEmpty()) {
            return;
        }

        List<JRBand> bands = new ArrayList<>();
        if (design.getTitle() != null) bands.add((JRBand) design.getTitle());
        if (design.getPageHeader() != null) bands.add((JRBand) design.getPageHeader());
        if (design.getColumnHeader() != null) bands.add((JRBand) design.getColumnHeader());
        
        if (design.getDetailSection() != null) {
            JRBand[] detailBands = design.getDetailSection().getBands();
            if (detailBands != null) {
                for (JRBand detailBand : detailBands) {
                    if (detailBand != null) {
                        bands.add(detailBand);
                    }
                }
            }
        }
        if (design.getColumnFooter() != null) bands.add((JRBand) design.getColumnFooter());
        if (design.getPageFooter() != null) bands.add((JRBand) design.getPageFooter());
        if (design.getSummary() != null) bands.add((JRBand) design.getSummary());

        for (JRBand band : bands) {
            for (JRElement element : band.getElements()) {
                String key = element.getKey();
                if (key != null && positions.containsKey(key)) {
                    Map<String, Integer> coords = positions.get(key);
                    if (element instanceof JRDesignElement) {
                        JRDesignElement de = (JRDesignElement) element;
                        if (coords.containsKey("x")) de.setX(coords.get("x"));
                        if (coords.containsKey("y")) de.setY(coords.get("y"));
                        if (coords.containsKey("width")) de.setWidth(coords.get("width"));
                        if (coords.containsKey("height")) de.setHeight(coords.get("height"));
                    }
                }
            }
        }
    }

    private static Map<String, Object> buildInvoiceParams(Invoice invoice) {
        Map<String, Object> params = new HashMap<>();
        params.put("invoiceNo", nvl(invoice.getInvoiceNo(), "N/A"));
        params.put("clientName", nvl(invoice.getClientName(), "N/A"));
        params.put("amount", invoice.getAmount() != null
                ? invoice.getAmount().toPlainString()
                : "0.00");
        params.put("issueDate", invoice.getIssueDate() != null
                ? invoice.getIssueDate().toString()
                : "");
        params.put("dueDate", invoice.getDueDate() != null
                ? invoice.getDueDate().toString()
                : "");
        params.put("status", invoice.getStatus() != null
                ? invoice.getStatus().name()
                : "Unpaid");
        params.put("notes", nvl(invoice.getNotes(), ""));
        return params;
    }

    private static Map<String, Object> buildChequeParams(Cheque cheque) {
        Map<String, Object> params = new HashMap<>();
        params.put("chequeNo", nvl(cheque.getChequeNo(), "N/A"));
        params.put("payeeName", nvl(cheque.getPayeeName(), "N/A"));
        params.put("amount", cheque.getAmount() != null
                ? cheque.getAmount().toPlainString()
                : "0.00");
        params.put("amountWords", nvl(cheque.getAmountWords(), ""));
        params.put("bankName", nvl(cheque.getBankName(), ""));
        params.put("issueDate", cheque.getIssueDate() != null
                ? cheque.getIssueDate().toString()
                : "");
        // signature path for Jasper templates (optional)
        try {
            if (com.chequeprint.util.SignatureService.hasSignature()) {
                params.put("signaturePath", com.chequeprint.util.SignatureService.getSignaturePath().toString());
            } else {
                params.put("signaturePath", "");
            }
        } catch (Exception ignored) {
            params.put("signaturePath", "");
        }
        // QR Code path for cheque verification
        try {
            String qrPath = com.chequeprint.util.QrCodeGenerator.generateQrCode(cheque);
            params.put("qrCodePath", qrPath != null ? qrPath : "");
        } catch (Exception e) {
            e.printStackTrace();
            params.put("qrCodePath", "");
        }
        return params;
    }

    private static List<String> resolveChequeTemplateCandidates(Cheque cheque, Bank bankTemplate) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        String explicitTemplatePath = bankTemplate != null ? bankTemplate.getLogoPath() : null;
        if (explicitTemplatePath != null) {
            String trimmed = explicitTemplatePath.trim();
            if (!trimmed.isEmpty() && trimmed.toLowerCase(Locale.ROOT).endsWith(".jrxml")) {
                File file = new File(trimmed);
                if (file.isFile()) {
                    candidates.add("file:" + file.getAbsolutePath());
                }
                if (trimmed.startsWith("/")) {
                    candidates.add(trimmed);
                } else {
                    candidates.add("/reports/" + trimmed);
                    candidates.add("/" + trimmed);
                }
            }
        }

        String bankName = bankTemplate != null && bankTemplate.getBankName() != null
                ? bankTemplate.getBankName()
                : cheque.getBankName();
        String bankCode = bankTemplate != null ? bankTemplate.getBankCode() : null;

        addBankTemplateCandidates(candidates, bankName);
        addBankTemplateCandidates(candidates, bankCode);
        candidates.add(DEFAULT_CHEQUE_TEMPLATE);

        return new ArrayList<>(candidates);
    }

    private static void addBankTemplateCandidates(LinkedHashSet<String> candidates, String token) {
        String slug = slugify(token);
        if (slug.isEmpty()) {
            return;
        }
        candidates.add("/reports/cheque_report_" + slug + ".jrxml");
        candidates.add("/reports/cheque_" + slug + ".jrxml");
    }

    private static String slugify(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
