package com.chequeprint.service;

import com.chequeprint.config.AppConfig;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChequeOcrService {

    private final ITesseract tesseract;

    public ChequeOcrService() {
        Tesseract t = new Tesseract();
        // datapath: either configured in AppConfig or fallback to TESSDATA_PREFIX env
        String dataPath = AppConfig.getTessDataPath();
        if (dataPath == null || dataPath.isBlank()) {
            dataPath = System.getenv("TESSDATA_PREFIX");
        }
        if (dataPath != null && !dataPath.isBlank()) {
            t.setDatapath(dataPath);
        }
        t.setLanguage("eng");
        this.tesseract = t;
    }

    public ChequeOcrResult extractFromFile(File file) throws OcrException {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) throw new OcrException("Unable to read image file.");
            BufferedImage pre = preprocessImage(img);
            String raw = tesseract.doOCR(pre);
            return parseRawText(raw);
        } catch (IOException | TesseractException e) {
            throw new OcrException("OCR extraction failed", e);
        }
    }

    private BufferedImage preprocessImage(BufferedImage in) {
        // convert to grayscale
        BufferedImage gray = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp op = new ColorConvertOp(in.getColorModel().getColorSpace(), gray.getColorModel().getColorSpace(), null);
        op.filter(in, gray);

        // increase contrast/brightness
        RescaleOp rescaleOp = new RescaleOp(1.2f, 15f, null);
        BufferedImage res = rescaleOp.filter(gray, null);

        // resize if too large for better OCR
        int max = 1600;
        if (res.getWidth() > max) {
            double scale = (double) max / res.getWidth();
            int w = max;
            int h = (int) (res.getHeight() * scale);
            Image tmp = res.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2 = resized.createGraphics();
            g2.drawImage(tmp, 0, 0, null);
            g2.dispose();
            return resized;
        }
        return res;
    }

    private ChequeOcrResult parseRawText(String raw) {
        String text = raw == null ? "" : raw.replaceAll("\r", "\n");

        String chequeNo = extractChequeNumber(text).orElse(null);
        BigDecimal amount = extractAmount(text).orElse(null);
        LocalDate date = extractDate(text).orElse(null);
        String name = extractPayee(text).orElse(null);

        return new ChequeOcrResult(name, amount, date, chequeNo, text);
    }

    private Optional<String> extractChequeNumber(String text) {
        // common patterns: CHQ-1234, Cheque No: 1234, Cheque #: 1234
        Pattern p = Pattern.compile("(CHQ[-\\s]?\\d{2,}|Cheque\\s*(No|#|Number)[:\\s]*([A-Za-z0-9-]+))", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String g = m.group();
            // cleanup
            g = g.replaceAll("(?i)Cheque\\s*(No|#|Number)[:\\s]*", "").trim();
            return Optional.of(g);
        }
        // fallback: any 4-10 digit token
        p = Pattern.compile("\\b(\\d{4,10})\\b");
        m = p.matcher(text);
        if (m.find()) return Optional.of(m.group(1));
        return Optional.empty();
    }

    private Optional<BigDecimal> extractAmount(String text) {
        // look for lines with rupee symbol or currency or Amount
        Pattern p = Pattern.compile("[₹$]?\\s*([0-9]{1,3}(?:[,\\.]?[0-9]{3})*(?:[.,][0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        BigDecimal best = null;
        while (m.find()) {
            String s = m.group(1).replaceAll(",", "").replaceAll("\\s+", "");
            try {
                BigDecimal v = new BigDecimal(s.replaceAll("\\.$", ""));
                if (best == null || v.compareTo(best) > 0) best = v; // prefer larger amounts
            } catch (Exception ignored) {}
        }
        return Optional.ofNullable(best);
    }

    private Optional<LocalDate> extractDate(String text) {
        // try common date formats
        Pattern p = Pattern.compile("(\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b)|(\\b\\d{4}-\\d{1,2}-\\d{1,2}\\b)");
        Matcher m = p.matcher(text);
        DateTimeFormatter[] fmts = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("d/M/uu"),
                DateTimeFormatter.ofPattern("d-M-uu"),
                DateTimeFormatter.ofPattern("uuuu-M-d")
        };
        if (m.find()) {
            String candidate = m.group();
            candidate = candidate.replaceAll("\\s", "");
            for (var fmt : fmts) {
                try {
                    LocalDate dt = LocalDate.parse(candidate, fmt.withResolverStyle(java.time.format.ResolverStyle.SMART));
                    return Optional.of(dt);
                } catch (DateTimeParseException ignored) {}
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractPayee(String text) {
        // look for 'Pay to the order of' or 'Pay to'
        Pattern p = Pattern.compile("(?i)(Pay to the order of|Pay to)[:\\s]*([A-Za-z \\.'&,-]{3,60})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String name = m.group(2).trim();
            // strip trailing words like 'Rupees' or amounts
            name = name.replaceAll("(Rupees|Rs\\.?|INR).*", "").trim();
            return Optional.of(name);
        }

        // fallback: take first non-empty line with letters
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.length() > 3 && l.matches(".*[A-Za-z].*")) {
                // avoid lines that look like dates or amounts
                if (!l.matches(".*\\d{2,}.*") || l.length() > 20) {
                    return Optional.of(l);
                }
            }
        }
        return Optional.empty();
    }
}
