package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.LayoutField;
import com.chequeprint.model.FieldPosition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Transform;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders cheque visual layout to a high-resolution snapshot for print preview and print job execution.
 */
public class ChequeSnapshotRenderer {

    public static BankTemplateLayout resolveLayout(Bank bankTemplate) {
        BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();
        BankTemplateLayout fromSize = ChequeSizeCodec.decodeLayout(bankTemplate != null ? bankTemplate.getChequeSize() : null);
        if (bankTemplate == null || bankTemplate.getBankCode() == null || bankTemplate.getBankCode().isBlank()) {
            fromSize.ensureAllFields();
            return fromSize;
        }

        String code = bankTemplate.getBankCode().trim().toUpperCase();
        Map<String, BankTemplateLayout> all = layoutStore.loadAll();
        BankTemplateLayout stored = all.get(code);
        if (stored == null) {
            fromSize.ensureAllFields();
            return fromSize;
        }

        BankTemplateLayout merged = stored.copy();
        merged.setWidthInches(fromSize.getWidthInches());
        merged.setHeightInches(fromSize.getHeightInches());
        merged.ensureAllFields();
        return merged;
    }

    public static WritableImage renderCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, double scale) {
        if (!Platform.isFxApplicationThread()) {
            AtomicReference<WritableImage> ref = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    ref.set(renderChequeInternal(cheque, bank, layout, scale));
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ref.get();
        } else {
            return renderChequeInternal(cheque, bank, layout, scale);
        }
    }

    private static WritableImage renderChequeInternal(Cheque cheque, Bank bank, BankTemplateLayout layout, double scale) {
        double widthPx = layout.getWidthInches() * 150.0; // base width for rendering
        double heightPx = layout.getHeightInches() * 150.0; // base height for rendering

        Pane pane = new Pane();
        pane.setPrefSize(widthPx, heightPx);
        pane.setMinSize(widthPx, heightPx);
        pane.setMaxSize(widthPx, heightPx);
        pane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000; -fx-border-width: 1px;");

        // Add visual fields matching layout field ratios
        for (LayoutField field : LayoutField.values()) {
            FieldPosition pos = layout.get(field);
            double xr = pos.getXRatio();
            double yr = pos.getYRatio();
            double wr = pos.getWidthRatio();
            double hr = pos.getHeightRatio();

            if (wr <= 0.0) wr = getDefaultWidthRatio(field);
            if (hr <= 0.0) hr = getDefaultHeightRatio(field);

            double x = xr * widthPx;
            double y = yr * heightPx;
            double w = wr * widthPx;
            double h = hr * heightPx;

            switch (field) {
                case DATE -> {
                    Label dateLbl = new Label(cheque.getIssueDate() != null ? cheque.getIssueDate().toString() : "");
                    dateLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
                    dateLbl.setTextFill(Color.BLACK);
                    dateLbl.setAlignment(Pos.CENTER);
                    dateLbl.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1px 0;");
                    dateLbl.setLayoutX(x);
                    dateLbl.setLayoutY(y);
                    dateLbl.setPrefSize(w, h);
                    pane.getChildren().add(dateLbl);
                }
                case PAYEE -> {
                    Label payeeLbl = new Label("Pay: " + (cheque.getPayeeName() != null ? cheque.getPayeeName() : ""));
                    payeeLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                    payeeLbl.setTextFill(Color.BLACK);
                    payeeLbl.setAlignment(Pos.CENTER_LEFT);
                    payeeLbl.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1px 0; -fx-padding: 0 0 0 5;");
                    payeeLbl.setLayoutX(x);
                    payeeLbl.setLayoutY(y);
                    payeeLbl.setPrefSize(w, h);
                    pane.getChildren().add(payeeLbl);
                }
                case AMOUNT_NUMBER -> {
                    Label amountLbl = new Label("₹ " + (cheque.getAmount() != null ? cheque.getAmount().toPlainString() : ""));
                    amountLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    amountLbl.setTextFill(Color.BLACK);
                    amountLbl.setAlignment(Pos.CENTER);
                    amountLbl.setStyle("-fx-border-color: #000000; -fx-border-width: 1px; -fx-background-color: #fefce8;");
                    amountLbl.setLayoutX(x);
                    amountLbl.setLayoutY(y);
                    amountLbl.setPrefSize(w, h);
                    pane.getChildren().add(amountLbl);
                }
                case AMOUNT_WORDS -> {
                    Label wordsLbl = new Label("Rupees: " + (cheque.getAmountWords() != null ? cheque.getAmountWords() : ""));
                    wordsLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
                    wordsLbl.setTextFill(Color.BLACK);
                    wordsLbl.setAlignment(Pos.CENTER_LEFT);
                    wordsLbl.setWrapText(true);
                    wordsLbl.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1px 0; -fx-padding: 0 0 0 5;");
                    wordsLbl.setLayoutX(x);
                    wordsLbl.setLayoutY(y);
                    wordsLbl.setPrefSize(w, h);
                    pane.getChildren().add(wordsLbl);
                }
                case SIGNATURE -> {
                    Pane sigPane = new Pane();
                    sigPane.setLayoutX(x);
                    sigPane.setLayoutY(y);
                    sigPane.setPrefSize(w, h);

                    try {
                        if (SignatureService.hasSignature()) {
                            ImageView sigImage = new ImageView(new Image(SignatureService.getSignaturePath().toUri().toString()));
                            sigImage.setFitWidth(w);
                            sigImage.setFitHeight(h - 15);
                            sigImage.setPreserveRatio(true);
                            sigPane.getChildren().add(sigImage);
                        }
                    } catch (Exception ignored) {}

                    Label authLbl = new Label("Authorised Signature");
                    authLbl.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                    authLbl.setTextFill(Color.BLACK);
                    authLbl.setAlignment(Pos.CENTER);
                    authLbl.setPrefWidth(w);
                    authLbl.setLayoutY(h - 12);
                    sigPane.getChildren().add(authLbl);

                    pane.getChildren().add(sigPane);
                }
                case BANK_LOGO -> {
                    Label bankLbl = new Label(bank != null ? bank.getBankName().toUpperCase() : "BANK");
                    bankLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
                    bankLbl.setTextFill(Color.web("#1a56db"));
                    bankLbl.setAlignment(Pos.CENTER_LEFT);
                    bankLbl.setLayoutX(x);
                    bankLbl.setLayoutY(y);
                    bankLbl.setPrefSize(w, h);
                    pane.getChildren().add(bankLbl);
                }
                case MICR -> {
                    if (bank != null && bank.isMicr()) {
                        Label micrLbl = new Label("⑆ 000000 ⑆ 0000000000 ⑆");
                        micrLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
                        micrLbl.setTextFill(Color.BLACK);
                        micrLbl.setAlignment(Pos.CENTER);
                        micrLbl.setLayoutX(x);
                        micrLbl.setLayoutY(y);
                        micrLbl.setPrefSize(w, h);
                        pane.getChildren().add(micrLbl);
                    }
                }
            }
        }

        // Add QR Code if generated
        try {
            String qrPath = QrCodeGenerator.generateQrCode(cheque);
            if (qrPath != null) {
                ImageView qrView = new ImageView(new Image(new File(qrPath).toURI().toString()));
                qrView.setFitWidth(40);
                qrView.setFitHeight(40);
                qrView.setLayoutX(10);
                qrView.setLayoutY(heightPx - 50);
                pane.getChildren().add(qrView);
            }
        } catch (Exception ignored) {}

        // Add to a dummy Scene and trigger css application + layout pass
        Scene dummyScene = new Scene(pane);
        pane.applyCss();
        pane.layout();

        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(Transform.scale(scale, scale));
        params.setFill(Color.WHITE);

        return pane.snapshot(params, null);
    }

    public static boolean printSnapshot(WritableImage image, Printer printer, String jobName, BankTemplateLayout layout) {
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            return false;
        }
        job.getJobSettings().setJobName(jobName);

        PageLayout pageLayout = job.getPrinter().createPageLayout(
                javafx.print.Paper.A4,
                PageOrientation.LANDSCAPE,
                Printer.MarginType.DEFAULT);

        double targetW = layout.getWidthInches() * 72.0;
        double targetH = layout.getHeightInches() * 72.0;

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(Math.min(targetW, pageLayout.getPrintableWidth()));
        imageView.setFitHeight(Math.min(targetH, pageLayout.getPrintableHeight()));
        imageView.setPreserveRatio(true);

        boolean success = job.printPage(imageView);
        if (success) {
            job.endJob();
        }
        return success;
    }

    public static String exportSnapshotPdf(WritableImage image, String jobName, BankTemplateLayout layout) throws Exception {
        double widthPoints = layout.getWidthInches() * 72.0;
        double heightPoints = layout.getHeightInches() * 72.0;

        Path tempFile = Files.createTempFile("chequepro-pdf-", ".pdf");
        com.lowagie.text.Document document = new com.lowagie.text.Document(
                new com.lowagie.text.Rectangle((float) widthPoints, (float) heightPoints), 0, 0, 0, 0);

        FileOutputStream fos = new FileOutputStream(tempFile.toFile());
        com.lowagie.text.pdf.PdfWriter.getInstance(document, fos);
        document.open();

        BufferedImage bufImg = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufImg, "png", baos);

        com.lowagie.text.Image pdfImg = com.lowagie.text.Image.getInstance(baos.toByteArray());
        pdfImg.scaleAbsolute((float) widthPoints, (float) heightPoints);
        document.add(pdfImg);

        document.close();
        fos.close();

        return tempFile.toAbsolutePath().toString();
    }

    private static double getDefaultWidthRatio(LayoutField field) {
        return switch (field) {
            case BANK_LOGO -> 0.18;
            case DATE -> 0.19;
            case PAYEE -> 0.66;
            case AMOUNT_NUMBER -> 0.16;
            case AMOUNT_WORDS -> 0.62;
            case SIGNATURE -> 0.22;
            case MICR -> 0.50;
        };
    }

    private static double getDefaultHeightRatio(LayoutField field) {
        return switch (field) {
            case BANK_LOGO -> 0.10;
            case DATE -> 0.10;
            case PAYEE -> 0.09;
            case AMOUNT_NUMBER -> 0.11;
            case AMOUNT_WORDS -> 0.09;
            case SIGNATURE -> 0.16;
            case MICR -> 0.08;
        };
    }
}
