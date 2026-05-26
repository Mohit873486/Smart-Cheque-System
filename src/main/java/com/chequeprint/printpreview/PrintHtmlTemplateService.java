package com.chequeprint.printpreview;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.Invoice;
import com.chequeprint.model.LayoutField;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.chequeprint.util.SignatureService;
import java.util.Properties;

public class PrintHtmlTemplateService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String buildChequeHtml(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        String template = loadTemplate("/templates/cheque-preview.html");

        double widthMm = layout.getWidthInches() * 25.4;
        double heightMm = layout.getHeightInches() * 25.4;

        template = template.replace("{{DOC_WIDTH_MM}}", formatMm(widthMm));
        template = template.replace("{{DOC_HEIGHT_MM}}", formatMm(heightMm));
        template = template.replace("{{BANK_NAME}}", escapeText(bankName(cheque, bank)));
        template = template.replace("{{CHEQUE_NO}}", escapeText(orDefault(cheque.getChequeNo(), "CHQ-")));
        template = template.replace("{{ISSUE_DATE}}", cheque.getIssueDate() != null ? escapeText(cheque.getIssueDate().format(DATE_FMT)) : "");
        template = template.replace("{{PAYEE}}", escapeText(orDefault(cheque.getPayeeName(), "-")));
        template = template.replace("{{AMOUNT_WORDS}}", escapeText(orDefault(cheque.getAmountWords(), "-")));
        template = template.replace("{{AMOUNT_NUMBER}}", escapeText(cheque.getAmount() != null ? cheque.getAmount().toPlainString() : "0.00"));
        template = template.replace("{{MICR_TEXT}}", escapeText(buildMicr(bank, cheque)));

        template = template.replace("{{DATE_LEFT}}", ratioToPercent(layout.get(LayoutField.DATE).getXRatio()));
        template = template.replace("{{DATE_TOP}}", ratioToPercent(layout.get(LayoutField.DATE).getYRatio()));

        template = template.replace("{{PAYEE_LEFT}}", ratioToPercent(layout.get(LayoutField.PAYEE).getXRatio()));
        template = template.replace("{{PAYEE_TOP}}", ratioToPercent(layout.get(LayoutField.PAYEE).getYRatio()));

        template = template.replace("{{WORDS_LEFT}}", ratioToPercent(layout.get(LayoutField.AMOUNT_WORDS).getXRatio()));
        template = template.replace("{{WORDS_TOP}}", ratioToPercent(layout.get(LayoutField.AMOUNT_WORDS).getYRatio()));

        template = template.replace("{{NUMBER_LEFT}}", ratioToPercent(layout.get(LayoutField.AMOUNT_NUMBER).getXRatio()));
        template = template.replace("{{NUMBER_TOP}}", ratioToPercent(layout.get(LayoutField.AMOUNT_NUMBER).getYRatio()));

        template = template.replace("{{SIGN_LEFT}}", ratioToPercent(layout.get(LayoutField.SIGNATURE).getXRatio()));
        template = template.replace("{{SIGN_TOP}}", ratioToPercent(layout.get(LayoutField.SIGNATURE).getYRatio()));

        // Signature injection: load saved metadata and signature path
        Properties meta = SignatureService.loadMetadata();
        boolean sigEnabled = Boolean.parseBoolean(meta.getProperty("enabled", "true")) && SignatureService.hasSignature();
        String sigSrc = sigEnabled ? SignatureService.getSignatureUrl() : "";
        String sigWidth = meta.getProperty("width", "120px");
        String sigHeight = meta.getProperty("height", "40px");
        String sigOffsetX = meta.getProperty("x", "0px");
        String sigOffsetY = meta.getProperty("y", "0px");
        String sigDisplay = sigEnabled ? "block" : "none";
        String sigLabelDisplay = sigEnabled ? "none" : "block";

        template = template.replace("{{SIGN_SRC}}", sigSrc);
        template = template.replace("{{SIGN_WIDTH}}", sigWidth);
        template = template.replace("{{SIGN_HEIGHT}}", sigHeight);
        template = template.replace("{{SIGN_DISPLAY}}", sigDisplay);
        template = template.replace("{{SIGN_LABEL_DISPLAY}}", sigLabelDisplay);
        template = template.replace("{{SIGN_OFFSET_X}}", sigOffsetX);
        template = template.replace("{{SIGN_OFFSET_Y}}", sigOffsetY);

        template = template.replace("{{MICR_LEFT}}", ratioToPercent(layout.get(LayoutField.MICR).getXRatio()));
        template = template.replace("{{MICR_TOP}}", ratioToPercent(layout.get(LayoutField.MICR).getYRatio()));

        return template;
    }

    public String buildInvoiceHtml(Invoice invoice) {
        String template = loadTemplate("/templates/invoice-preview.html");

        template = template.replace("{{INVOICE_NO}}", escapeText(orDefault(invoice.getInvoiceNo(), "INV-")));
        template = template.replace("{{CLIENT}}", escapeText(orDefault(invoice.getClientName(), "-")));
        template = template.replace("{{AMOUNT}}", escapeText(invoice.getAmount() != null ? invoice.getAmount().toPlainString() : "0.00"));
        template = template.replace("{{STATUS}}", escapeText(invoice.getStatus() != null ? invoice.getStatus().name() : "Unpaid"));
        template = template.replace("{{ISSUE_DATE}}", invoice.getIssueDate() != null ? escapeText(invoice.getIssueDate().format(DATE_FMT)) : "-");
        template = template.replace("{{DUE_DATE}}", invoice.getDueDate() != null ? escapeText(invoice.getDueDate().format(DATE_FMT)) : "-");
        template = template.replace("{{NOTES}}", escapeText(orDefault(invoice.getNotes(), "-")));

        return template;
    }

    private String buildMicr(Bank bank, Cheque cheque) {
        String code = bank != null ? orDefault(bank.getBankCode(), "") : "";
        String number = cheque.getChequeNo() != null ? cheque.getChequeNo().replaceAll("[^0-9]", "") : "";
        if (number.isEmpty()) {
            number = "000000000";
        }
        return String.format(Locale.ROOT, "C %s C %s C", number, code);
    }

    private String ratioToPercent(double ratio) {
        return String.format(Locale.ROOT, "%.2f%%", ratio * 100.0);
    }

    private String formatMm(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String bankName(Cheque cheque, Bank bank) {
        if (bank != null && bank.getBankName() != null && !bank.getBankName().isBlank()) {
            return bank.getBankName();
        }
        return orDefault(cheque.getBankName(), "Bank");
    }

    private String loadTemplate(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Template not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read template: " + resourcePath, ex);
        }
    }

    private String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String escapeText(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}
