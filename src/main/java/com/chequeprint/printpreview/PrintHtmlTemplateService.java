package com.chequeprint.printpreview;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.Invoice;
import com.chequeprint.model.LayoutField;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
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
        template = template.replace("{{ISSUE_DATE}}", cheque.getIssueDate() != null ? escapeText(cheque.getIssueDate().toString()) : "");
        template = template.replace("{{PAYEE}}", escapeText(orDefault(cheque.getPayeeName(), "-")));
        template = template.replace("{{AMOUNT_WORDS}}", escapeText(orDefault(cheque.getAmountWords(), "-")));
        template = template.replace("{{AMOUNT_NUMBER}}", escapeText(formatAmount(cheque.getAmount())));
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
        String sigSrc = sigEnabled ? buildSignatureDataUri() : "";
        String sigWidth = normalizeCssLength(meta.getProperty("width"), "120px", 40.0);
        String sigHeight = normalizeCssLength(meta.getProperty("height"), "40px", 24.0);
        String sigOffsetX = normalizeCssLength(meta.getProperty("x"), "0px", null);
        String sigOffsetY = normalizeCssLength(meta.getProperty("y"), "0px", null);
        String sigDisplay = sigEnabled ? "block" : "none";
        String sigLabelDisplay = "block";

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

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatMm(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String normalizeCssLength(String raw, String fallback, Double minPx) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            value = value + "px";
        }

        if (value.endsWith("px")) {
            try {
                double px = Double.parseDouble(value.substring(0, value.length() - 2).trim());
                if (minPx != null && px < minPx) {
                    return formatPx(minPx);
                }
                return formatPx(px);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        return value;
    }

    private String formatPx(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.ROOT, "%.0fpx", value);
        }
        return String.format(Locale.ROOT, "%.2fpx", value);
    }

    private String buildSignatureDataUri() {
        Path path = SignatureService.getSignaturePath();
        if (!Files.exists(path)) {
            return "";
        }

        try {
            BufferedImage source = ImageIO.read(path.toFile());
            if (source == null) {
                return SignatureService.getSignatureUrl();
            }

            BufferedImage trimmed = trimTransparentPadding(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(trimmed, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            return SignatureService.getSignatureUrl();
        }
    }

    private BufferedImage trimTransparentPadding(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = (source.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }

        int padding = 8;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(source.getWidth() - 1, maxX + padding);
        maxY = Math.min(source.getHeight() - 1, maxY + padding);

        BufferedImage sub = source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        BufferedImage copy = new BufferedImage(sub.getWidth(), sub.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(sub, 0, 0, null);
        g.dispose();
        return copy;
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
