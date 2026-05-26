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

    public static boolean printCheque(Cheque cheque) throws JRException {
        return printCheque(cheque, null);
    }

    public static boolean printCheque(Cheque cheque, Bank bankTemplate) throws JRException {
        JasperReport jr = compileChequeReport(cheque, bankTemplate);
        Map<String, Object> params = buildChequeParams(cheque);
        JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource());
        JasperPrintManager.printReport(print, false);
        cheque.setStatus(Cheque.Status.Printed);
        return true;
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
        InputStream template = JasperPrintUtil.class
                .getResourceAsStream("/reports/invoice_report.jrxml");

        if (template == null) {
            throw new JRException(
                    "invoice_report.jrxml not found in /reports/. Add the file to src/main/resources/reports/");
        }

        JasperReport jr = JasperCompileManager.compileReport(template);
        Map<String, Object> params = buildInvoiceParams(invoice);
        JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource());
        JasperPrintManager.printReport(print, false);
        return true;
    }

    public static String exportChequePdf(Cheque cheque, String outputDir) throws JRException {
        return exportChequePdf(cheque, outputDir, null);
    }

    public static String exportChequePdf(Cheque cheque, String outputDir, Bank bankTemplate) throws JRException {
        JasperReport jr = compileChequeReport(cheque, bankTemplate);
        Map<String, Object> params = buildChequeParams(cheque);

        JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource());

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String pdfPath = outputDir + File.separator + nvl(cheque.getChequeNo(), "cheque") + ".pdf";

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfPath));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataTitle("Cheque - " + nvl(cheque.getChequeNo(), ""));
        config.setMetadataAuthor("ChequePro v2.0");
        exporter.setConfiguration(config);
        exporter.exportReport();

        return pdfPath;
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

    private static JasperReport compileChequeReport(Cheque cheque, Bank bankTemplate) throws JRException {
        List<String> candidates = resolveChequeTemplateCandidates(cheque, bankTemplate);

        for (String candidate : candidates) {
            if (candidate.startsWith("file:")) {
                String path = candidate.substring("file:".length());
                File file = new File(path);
                if (file.isFile()) {
                    return JasperCompileManager.compileReport(path);
                }
                continue;
            }

            try (InputStream template = JasperPrintUtil.class.getResourceAsStream(candidate)) {
                if (template != null) {
                    return JasperCompileManager.compileReport(template);
                }
            } catch (Exception ex) {
                throw new JRException("Failed to compile cheque template: " + candidate, ex);
            }
        }

        throw new JRException("No cheque template found for bank '"
                + nvl(cheque.getBankName(), "Unknown")
                + "'. Tried: " + String.join(", ", candidates));
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
