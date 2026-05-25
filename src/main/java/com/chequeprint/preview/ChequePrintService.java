package com.chequeprint.preview;

import javax.swing.JOptionPane;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

public class ChequePrintService {

  public void testPrint(ChequePreviewPanel previewPanel) {
    try {
      PrinterJob job = PrinterJob.getPrinterJob();
      job.setJobName("Cheque Print");

      PageFormat format = createChequePageFormat(job);
      job.setPrintable(previewPanel, format);

      // Print directly to the default printer without showing the dialog.
      job.print();
    } catch (PrinterException ex) {
      JOptionPane.showMessageDialog(previewPanel,
          "Printing failed: " + ex.getMessage(),
          "Print Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  public PageFormat createChequePageFormat(PrinterJob job) {
    PageFormat base = job.defaultPage();
    Paper paper = new Paper();

    double widthPt = ChequeLayout.WIDTH_INCH * 72.0;
    double heightPt = ChequeLayout.HEIGHT_INCH * 72.0;
    double marginInches = 0.1;
    double marginPt = marginInches * 72.0;

    paper.setSize(widthPt, heightPt);
    paper.setImageableArea(marginPt, marginPt,
        widthPt - marginPt * 2.0,
        heightPt - marginPt * 2.0);

    base.setOrientation(PageFormat.PORTRAIT);
    base.setPaper(paper);
    return base;
  }
}
