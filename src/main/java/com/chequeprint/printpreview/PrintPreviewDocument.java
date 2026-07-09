package com.chequeprint.printpreview;

import javafx.print.Printer;

public final class PrintPreviewDocument {
    @FunctionalInterface
    public interface PdfSaveHandler {
        String savePdf() throws Exception;
    }

    @FunctionalInterface
    public interface PrintHandler {
        boolean print(Printer printer) throws Exception;
    }

    private final String windowTitle;
    private final String documentTitle;
    private final String jobName;
    private final String htmlContent;
    private final double widthMm;
    private final double heightMm;
    private final PrintHandler printHandler;
    private final PdfSaveHandler pdfSaveHandler;

    public PrintPreviewDocument(
            String windowTitle,
            String documentTitle,
            String jobName,
            String htmlContent,
            double widthMm,
            double heightMm,
            PrintHandler printHandler,
            PdfSaveHandler pdfSaveHandler) {
        this.windowTitle = windowTitle;
        this.documentTitle = documentTitle;
        this.jobName = jobName;
        this.htmlContent = htmlContent;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.printHandler = printHandler;
        this.pdfSaveHandler = pdfSaveHandler;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public String getJobName() {
        return jobName;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public double getWidthMm() {
        return widthMm;
    }

    public double getHeightMm() {
        return heightMm;
    }

    public PrintHandler getPrintHandler() {
        return printHandler;
    }

    public PdfSaveHandler getPdfSaveHandler() {
        return pdfSaveHandler;
    }
}
