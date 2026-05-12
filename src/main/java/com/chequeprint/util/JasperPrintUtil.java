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
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

        JasperPrintManager.printReport(print, true);

        cheque.setStatus(Cheque.Status.Printed);
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