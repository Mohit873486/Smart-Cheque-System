package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BankTemplatePdfExporter {

    private BankTemplatePdfExporter() {
    }

    public static String export(Bank bank, BankTemplateLayout layout, String outputDir) throws Exception {
        if (bank == null || layout == null) {
            throw new IllegalArgumentException("Bank and layout are required.");
        }
        if (outputDir == null || outputDir.isBlank()) {
            throw new IllegalArgumentException("Output directory is required.");
        }

        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);

        String safeCode = bank.getBankCode() == null ? "BANK" : bank.getBankCode().replaceAll("[^A-Za-z0-9_-]", "");
        if (safeCode.isBlank()) {
            safeCode = "BANK";
        }
        Path out = resolveOutputPath(dir, "template_" + safeCode.toUpperCase() + ".pdf");

        float widthPt = (float) (layout.getWidthInches() * 72.0);
        float heightPt = (float) (layout.getHeightInches() * 72.0);

        Document doc = new Document(new Rectangle(widthPt, heightPt));
        try (FileOutputStream os = new FileOutputStream(out.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(doc, os);
            doc.open();

            PdfContentByte cb = writer.getDirectContent();
            BaseFont font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

            cb.setColorFill(Color.WHITE);
            cb.rectangle(0, 0, widthPt, heightPt);
            cb.fill();

            cb.setColorStroke(new Color(80, 93, 119));
            cb.setLineWidth(1.2f);
            cb.roundRectangle(1.5f, 1.5f, widthPt - 3f, heightPt - 3f, 10f);
            cb.stroke();

            cb.setColorFill(new Color(230, 236, 246));
            cb.rectangle(0, heightPt - 28f, widthPt, 28f);
            cb.fill();

            cb.setColorFill(Color.BLACK);
            cb.beginText();
            cb.setFontAndSize(font, 11f);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, bank.getBankName() + " - Template", 10f, heightPt - 18f, 0f);
            cb.endText();

            drawField(cb, font, "DATE", layout.get(LayoutField.DATE), widthPt, heightPt, PdfContentByte.ALIGN_LEFT);
            drawField(cb, font, "PAY TO", layout.get(LayoutField.PAYEE), widthPt, heightPt, PdfContentByte.ALIGN_LEFT);
            drawField(cb, font, "AMOUNT", layout.get(LayoutField.AMOUNT_NUMBER), widthPt, heightPt, PdfContentByte.ALIGN_LEFT);
            drawField(cb, font, "AMOUNT IN WORDS", layout.get(LayoutField.AMOUNT_WORDS), widthPt, heightPt, PdfContentByte.ALIGN_LEFT);
            drawField(cb, font, "SIGNATURE", layout.get(LayoutField.SIGNATURE), widthPt, heightPt, PdfContentByte.ALIGN_LEFT);
            drawField(cb, font, "BANK LOGO", layout.get(LayoutField.BANK_LOGO), widthPt, heightPt, PdfContentByte.ALIGN_LEFT);

            if (bank.isMicr()) {
                FieldPosition micr = layout.get(LayoutField.MICR);
                float mx = ratioToX(micr.getXRatio(), widthPt);
                float my = ratioToY(micr.getYRatio(), heightPt);
                cb.setColorFill(new Color(245, 247, 252));
                cb.rectangle(8f, my - 8f, widthPt - 16f, 18f);
                cb.fill();
                cb.setColorFill(Color.DARK_GRAY);
                cb.beginText();
                cb.setFontAndSize(font, 9f);
                cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "MICR: |: 000000 :| 0000000000 |: 00", mx, my, 0f);
                cb.endText();
            }
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }

        return out.toAbsolutePath().toString();
    }

    private static void drawField(PdfContentByte cb, BaseFont font, String text, FieldPosition pos,
                                  float widthPt, float heightPt, int align) {
        float x = ratioToX(pos.getXRatio(), widthPt);
        float y = ratioToY(pos.getYRatio(), heightPt);

        cb.setColorStroke(new Color(158, 170, 196));
        cb.setLineWidth(0.5f);
        cb.rectangle(x - 2f, y - 9f, Math.min(130f, widthPt * 0.35f), 13f);
        cb.stroke();

        cb.setColorFill(Color.BLACK);
        cb.beginText();
        cb.setFontAndSize(font, 8f);
        cb.showTextAligned(align, text, x, y, 0f);
        cb.endText();
    }

    private static float ratioToX(double ratio, float width) {
        return (float) Math.max(8f, Math.min(width - 8f, ratio * width));
    }

    private static float ratioToY(double ratio, float height) {
        float topBased = (float) (ratio * height);
        return Math.max(8f, Math.min(height - 8f, height - topBased));
    }

    private static Path resolveOutputPath(Path dir, String fileName) throws IOException {
        Path out = dir.resolve(fileName);
        if (!Files.exists(out)) {
            return out;
        }

        String base = fileName.endsWith(".pdf") ? fileName.substring(0, fileName.length() - 4) : fileName;
        for (int i = 1; i < 1000; i++) {
            Path candidate = dir.resolve(base + "_" + i + ".pdf");
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new IOException("Unable to create a unique PDF file name in: " + dir);
    }
}
