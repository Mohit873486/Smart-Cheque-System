
package com.chequeprint.util;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;

public class JasperPrintUtil {

    // ── Print cheque — shows system print dialog ─────────────────────
    public static void printCheque(Cheque cheque) throws JRException {
        InputStream template = JasperPrintUtil.class
                .getResourceAsStream("/reports/cheque_report.jrxml");

        if (template == null)
            throw new JRException(
                "cheque_report.jrxml not found in /reports/. "
              + "Add the file to src/main/resources/reports/");

        JasperReport jr = JasperCompileManager.compileReport(template);

        Map<String, Object> params = new HashMap<>();
        params.put("chequeNo",    nvl(cheque.getChequeNo(),    "N/A"));
        params.put("payeeName",   nvl(cheque.getPayeeName(),   "N/A"));
        params.put("amount",      cheque.getAmount() != null
                                      ? cheque.getAmount().toPlainString() : "0.00");
        params.put("amountWords", nvl(cheque.getAmountWords(), ""));
        params.put("bankName",    nvl(cheque.getBankName(),    ""));
        params.put("issueDate",   cheque.getIssueDate() != null
                                      ? cheque.getIssueDate().toString() : "");

        JasperPrint print = JasperFillManager.fillReport(
                jr, params, new JREmptyDataSource());

        // Use JRPrintServiceExporter to control print attributes and paper size.
        try {
            // compute page size in millimeters from JR report size (points)
            int pageWidthPts = jr.getPageWidth();
            int pageHeightPts = jr.getPageHeight();
            // 1 inch = 72 points; 1 inch = 25.4 mm
            double widthMm = pageWidthPts * 25.4 / 72.0;
            double heightMm = pageHeightPts * 25.4 / 72.0;

            PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
            // prefer default media, but set printable area to match report
            printRequestAttributeSet.add(new Copies(1));
            // MediaPrintableArea(x, y, w, h, units) — use full page area
            printRequestAttributeSet.add(new MediaPrintableArea(0f, 0f, (float) widthMm, (float) heightMm,
                    MediaPrintableArea.MM));
            // optionally hint A4 for printers that require a media name
            printRequestAttributeSet.add(MediaSizeName.ISO_A4);

            JRPrintServiceExporter exporter = new JRPrintServiceExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));

            SimplePrintServiceExporterConfiguration cfg = new SimplePrintServiceExporterConfiguration();
            cfg.setPrintRequestAttributeSet(printRequestAttributeSet);
            cfg.setDisplayPageDialog(true);
            cfg.setDisplayPrintDialog(true);

            // try to use default print service
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService != null) {
                cfg.setPrintService(defaultService);
            }

            exporter.setConfiguration(cfg);
            exporter.exportReport();

            cheque.setStatus(Cheque.Status.Printed);
        } catch (Exception ex) {
            // fallback to default behavior (shows dialog)
            JasperPrintManager.printReport(print, true);
            cheque.setStatus(Cheque.Status.Printed);
        }
    }

    // ── Export cheque as PDF file ────────────────────────────────────
    public static String exportChequePdf(Cheque cheque, String outputDir)
            throws JRException {

        InputStream template = JasperPrintUtil.class
                .getResourceAsStream("/reports/cheque_report.jrxml");

        if (template == null)
            throw new JRException("cheque_report.jrxml not found in /reports/");

        JasperReport jr = JasperCompileManager.compileReport(template);

        Map<String, Object> params = new HashMap<>();
        params.put("chequeNo",    nvl(cheque.getChequeNo(),    "N/A"));
        params.put("payeeName",   nvl(cheque.getPayeeName(),   "N/A"));
        params.put("amount",      cheque.getAmount() != null
                                      ? cheque.getAmount().toPlainString() : "0.00");
        params.put("amountWords", nvl(cheque.getAmountWords(), ""));
        params.put("bankName",    nvl(cheque.getBankName(),    ""));
        params.put("issueDate",   cheque.getIssueDate() != null
                                      ? cheque.getIssueDate().toString() : "");

        JasperPrint print = JasperFillManager.fillReport(
                jr, params, new JREmptyDataSource());

        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String pdfPath = outputDir + File.separator
                + nvl(cheque.getChequeNo(), "cheque") + ".pdf";

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfPath));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataTitle("Cheque — " + nvl(cheque.getChequeNo(), ""));
        config.setMetadataAuthor("ChequePro v2.0");
        exporter.setConfiguration(config);
        exporter.exportReport();

        return pdfPath;
    }

    // ── Export invoice as PDF file ───────────────────────────────────
    public static String exportInvoicePdf(Invoice invoice, String outputDir)
            throws JRException {

        InputStream template = JasperPrintUtil.class
                .getResourceAsStream("/reports/invoice_report.jrxml");

        if (template == null)
            throw new JRException(
                "invoice_report.jrxml not found in /reports/. "
              + "Add the file to src/main/resources/reports/");

        JasperReport jr = JasperCompileManager.compileReport(template);

        Map<String, Object> params = new HashMap<>();
        params.put("invoiceNo",  nvl(invoice.getInvoiceNo(),  "N/A"));
        params.put("clientName", nvl(invoice.getClientName(), "N/A"));
        params.put("amount",     invoice.getAmount() != null
                                     ? invoice.getAmount().toPlainString() : "0.00");
        params.put("issueDate",  invoice.getIssueDate() != null
                                     ? invoice.getIssueDate().toString() : "");
        params.put("dueDate",    invoice.getDueDate() != null
                                     ? invoice.getDueDate().toString() : "");
        params.put("status",     invoice.getStatus() != null
                                     ? invoice.getStatus().name() : "Unpaid");
        params.put("notes",      nvl(invoice.getNotes(), ""));

        JasperPrint print = JasperFillManager.fillReport(
                jr, params, new JREmptyDataSource());

        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String pdfPath = outputDir + File.separator
                + nvl(invoice.getInvoiceNo(), "invoice") + ".pdf";

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfPath));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataTitle("Invoice — " + nvl(invoice.getInvoiceNo(), ""));
        config.setMetadataAuthor("ChequePro v2.0");
        exporter.setConfiguration(config);
        exporter.exportReport();

        return pdfPath;
    }

    // ── Helper ───────────────────────────────────────────────────────
    private static String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}