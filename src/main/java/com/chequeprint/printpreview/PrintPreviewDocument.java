package com.chequeprint.printpreview;

public final class PrintPreviewDocument {
    @FunctionalInterface
    public interface PdfSaveHandler {
        String savePdf() throws Exception;
    }

    private final String windowTitle;
    private final String documentTitle;
    private final String jobName;
    private final String htmlContent;
    private final double widthMm;
    private final double heightMm;
    private final PdfSaveHandler pdfSaveHandler;

    public PrintPreviewDocument(
            String windowTitle,
            String documentTitle,
            String jobName,
            String htmlContent,
            double widthMm,
            double heightMm,
            PdfSaveHandler pdfSaveHandler) {
        this.windowTitle = windowTitle;
        this.documentTitle = documentTitle;
        this.jobName = jobName;
        this.htmlContent = htmlContent;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
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

    public PdfSaveHandler getPdfSaveHandler() {
        return pdfSaveHandler;
    }
}
