package com.chequeprint.preview;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class ChequePreviewPanel extends JPanel implements Printable {
  private final ChequeRenderer renderer = new ChequeRenderer();

  private BankTemplate template;
  private ChequeData chequeData = new ChequeData();

  public ChequePreviewPanel() {
    setBackground(new Color(0x0F172A));
    setPreferredSize(new Dimension(1000, 460));
  }

  public void setBankTemplate(BankTemplate template) {
    this.template = template;
    repaint();
  }

  public BankTemplate getBankTemplate() {
    return template;
  }

  public void setChequeData(ChequeData data) {
    this.chequeData = data != null ? data : new ChequeData();
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    if (template == null) {
      return;
    }

    Graphics2D g2 = (Graphics2D) graphics.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    double scale = Math.min((double) getWidth() / ChequeLayout.CANVAS_WIDTH,
        (double) getHeight() / ChequeLayout.CANVAS_HEIGHT);

    double tx = (getWidth() - ChequeLayout.CANVAS_WIDTH * scale) / 2.0;
    double ty = (getHeight() - ChequeLayout.CANVAS_HEIGHT * scale) / 2.0;

    g2.translate(tx, ty);
    g2.scale(scale, scale);

    renderer.render(g2, chequeData, template, true);

    g2.setColor(new Color(255, 255, 255, 25));
    g2.setStroke(new BasicStroke(4f));
    g2.drawRect(0, 0, ChequeLayout.CANVAS_WIDTH, ChequeLayout.CANVAS_HEIGHT);
    g2.dispose();
  }

  @Override
  public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
    if (pageIndex > 0 || template == null) {
      return NO_SUCH_PAGE;
    }

    Graphics2D g2 = (Graphics2D) graphics.create();

    double imageableW = pageFormat.getImageableWidth();
    double imageableH = pageFormat.getImageableHeight();

    double sx = imageableW / ChequeLayout.CANVAS_WIDTH;
    double sy = imageableH / ChequeLayout.CANVAS_HEIGHT;
    double scale = Math.min(sx, sy);

    double tx = pageFormat.getImageableX() + ((imageableW - ChequeLayout.CANVAS_WIDTH * scale) / 2.0);
    double ty = pageFormat.getImageableY() + ((imageableH - ChequeLayout.CANVAS_HEIGHT * scale) / 2.0);

    g2.translate(tx, ty);
    g2.scale(scale, scale);

    renderer.render(g2, chequeData, template, false);
    g2.dispose();
    return PAGE_EXISTS;
  }
}
