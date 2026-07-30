package com.chequeprint.preview;

import com.chequeprint.service.PrintService;

/**
 * @deprecated Merged into single {@link PrintService}. Use PrintService directly.
 */
@Deprecated
public class ChequePrintService {

  private final PrintService printService = new PrintService();

  public void testPrint(ChequePreviewPanel previewPanel) {
    try {
      printService.validatePrinter();
    } catch (Exception ex) {
      javax.swing.JOptionPane.showMessageDialog(previewPanel,
          "Printing failed: " + ex.getMessage(),
          "Print Error",
          javax.swing.JOptionPane.ERROR_MESSAGE);
    }
  }

  public java.awt.print.PageFormat createChequePageFormat(java.awt.print.PrinterJob job) {
    java.awt.print.PageFormat base = job.defaultPage();
    java.awt.print.Paper paper = new java.awt.print.Paper();

    double widthPt = ChequeLayout.WIDTH_INCH * 72.0;
    double heightPt = ChequeLayout.HEIGHT_INCH * 72.0;
    double marginInches = 0.1;
    double marginPt = marginInches * 72.0;

    paper.setSize(widthPt, heightPt);
    paper.setImageableArea(marginPt, marginPt,
        widthPt - marginPt * 2.0,
        heightPt - marginPt * 2.0);

    base.setOrientation(java.awt.print.PageFormat.PORTRAIT);
    base.setPaper(paper);
    return base;
  }
}
