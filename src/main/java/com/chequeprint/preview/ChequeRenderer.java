package com.chequeprint.preview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

public class ChequeRenderer {
  private static final Color PAPER = new Color(0xFFFFFF);
  private static final Color GRID = new Color(0xD6DBE3);
  private static final Color TEXT = new Color(0x111827);
  private static final Color SUBTLE = new Color(0x6B7280);
  private static final int ARC = 42;

  public void render(Graphics2D g2, ChequeData data, BankTemplate template, boolean includeGuideBoxes) {
    Objects.requireNonNull(g2, "graphics");
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(template, "template");

    applyQuality(g2);

    int width = ChequeLayout.CANVAS_WIDTH;
    int height = ChequeLayout.CANVAS_HEIGHT;

    drawBackground(g2, template, width, height);
    drawGuides(g2, width, height);

    drawField(g2, "PAY", valueOrPreview(data.getPayeeName()), template.getPosition(ChequeField.PAYEE_NAME),
        width, false, includeGuideBoxes);
    drawField(g2, "DATE", data.getIssueDateText(), template.getPosition(ChequeField.DATE), width, true,
        includeGuideBoxes);
    drawField(g2, "RUPEES", valueOrPreview(data.getAmountWords()), template.getPosition(ChequeField.AMOUNT_WORDS),
        width, false, includeGuideBoxes);
    drawField(g2, "Rs.", valueOrPreview(data.getAmountNumber()), template.getPosition(ChequeField.AMOUNT_NUMBER),
        width, true, includeGuideBoxes);

    g2.setColor(new Color(0x344054));
    g2.setFont(new Font("Monospaced", Font.BOLD, 42));
    g2.drawString("|: 000000 :| 0000000000 |: 00", scaleX(145), scaleY(1000));
  }

  private void drawBackground(Graphics2D g2, BankTemplate template, int width, int height) {
    g2.setColor(PAPER);
    g2.fill(new RoundRectangle2D.Double(0, 0, width, height, ARC, ARC));

    g2.setColor(template.getAccentColor());
    g2.fillRoundRect(0, 0, width, scaleY(160), ARC, ARC);

    g2.setColor(new Color(255, 255, 255, 36));
    g2.fillRect(0, scaleY(95), width, scaleY(16));

    g2.setColor(new Color(0x3D4451));
    g2.setStroke(new BasicStroke(scaleX(6)));
    g2.draw(new RoundRectangle2D.Double(scaleX(4), scaleY(4), width - scaleX(8), height - scaleY(8), ARC, ARC));

    g2.setColor(Color.WHITE);
    g2.setFont(new Font("SansSerif", Font.BOLD, scaleY(58)));
    g2.drawString(template.getDisplayName(), scaleX(90), scaleY(108));

    g2.setColor(new Color(255, 255, 255, 210));
    g2.setFont(new Font("SansSerif", Font.PLAIN, scaleY(32)));
    g2.drawString("Cheque Printing Preview", scaleX(90), scaleY(145));
  }

  private void drawGuides(Graphics2D g2, int width, int height) {
    g2.setColor(GRID);
    g2.setStroke(new BasicStroke(scaleX(2)));
    g2.drawLine(scaleX(120), scaleY(220), width - scaleX(120), scaleY(220));
    g2.drawLine(scaleX(120), scaleY(560), width - scaleX(120), scaleY(560));
    g2.drawLine(scaleX(120), scaleY(860), width - scaleX(120), scaleY(860));

    g2.setColor(SUBTLE);
    g2.setFont(new Font("SansSerif", Font.PLAIN, scaleY(24)));
    g2.drawString("A/C Payee", scaleX(148), scaleY(254));
  }

  private void drawField(Graphics2D g2, String label, String value, FieldPosition pos, int canvasWidth, boolean rightAlign,
      boolean includeGuideBox) {
    int x = clamp(pos.getX(), 0, canvasWidth - scaleX(50));
    int y = clamp(pos.getY(), scaleY(180), ChequeLayout.CANVAS_HEIGHT - scaleY(110));

    g2.setFont(new Font("SansSerif", Font.BOLD, scaleY(24)));
    g2.setColor(SUBTLE);
    g2.drawString(label, x, y - scaleY(14));

    g2.setFont(new Font("SansSerif", Font.PLAIN, scaleY(36)));
    g2.setColor(TEXT);

    int textX = x;
    if (rightAlign) {
      int valueWidth = g2.getFontMetrics().stringWidth(value);
      textX = x - valueWidth;
    }
    g2.drawString(value, textX, y + scaleY(24));

    if (includeGuideBox) {
      g2.setColor(new Color(32, 83, 144, 65));
      g2.setStroke(new BasicStroke(scaleX(2)));
      int boxWidth = rightAlign ? scaleX(460) : scaleX(1240);
      int boxX = rightAlign ? x - boxWidth : x - scaleX(14);
      g2.drawRoundRect(boxX, y - scaleY(40), boxWidth, scaleY(70), scaleY(14), scaleY(14));
    }
  }

  private static String valueOrPreview(String text) {
    return text == null || text.isBlank() ? "[preview]" : text;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private static int scaleX(int valueAt300Dpi) {
    return valueAt300Dpi;
  }

  private static int scaleY(int valueAt300Dpi) {
    return valueAt300Dpi;
  }

  private static void applyQuality(Graphics2D g2) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
  }
}
