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

        String bankCode = (bank != null && bank.getBankCode() != null) ? bank.getBankCode().toUpperCase().trim() : "";

        Pane pane = new Pane();
        pane.setPrefSize(widthPx, heightPx);
        pane.setMinSize(widthPx, heightPx);
        pane.setMaxSize(widthPx, heightPx);

        // 1. Background gradient and styling
        String bgGradientStyle = switch (bankCode) {
            case "SBI" -> "-fx-background-color: linear-gradient(to bottom, #dbeafe, #bae6fd); -fx-border-color: #94a3b8; -fx-border-width: 1px;";
            case "HDFC" -> "-fx-background-color: linear-gradient(to bottom, #e0f2fe, #f0f9ff); -fx-border-color: #003366; -fx-border-width: 1px;";
            case "ICICI" -> "-fx-background-color: linear-gradient(to bottom, #ffedd5, #fed7aa); -fx-border-color: #ea580c; -fx-border-width: 1px;";
            case "AXIS" -> "-fx-background-color: linear-gradient(to bottom, #fce7f3, #fbcfe8); -fx-border-color: #db2777; -fx-border-width: 1px;";
            case "BOB", "BARODA" -> "-fx-background-color: linear-gradient(to bottom, #ffedd5, #eff6ff); -fx-border-color: #f05a28; -fx-border-width: 1px;";
            default -> "-fx-background-color: linear-gradient(to bottom, #eff6ff, #dbeafe); -fx-border-color: #2563eb; -fx-border-width: 1px;";
        };
        pane.setStyle(bgGradientStyle);

        // 2. Subtle horizontal security lines texture
        for (double ly = 15; ly < heightPx - 50; ly += 6) {
            javafx.scene.shape.Line secLine = new javafx.scene.shape.Line(10, ly, widthPx - 10, ly);
            secLine.setStroke(Color.web("#ffffff", 0.45));
            secLine.setStrokeWidth(0.5);
            pane.getChildren().add(secLine);
        }

        // 3. A/C Payee Crossing (diagonal stamp in top-left)
        javafx.scene.Group crossing = new javafx.scene.Group();
        javafx.scene.shape.Line crossLine1 = new javafx.scene.shape.Line(10, 50, 70, 10);
        crossLine1.setStroke(Color.web("#475569", 0.7));
        crossLine1.setStrokeWidth(1.0);
        javafx.scene.shape.Line crossLine2 = new javafx.scene.shape.Line(25, 50, 85, 10);
        crossLine2.setStroke(Color.web("#475569", 0.7));
        crossLine2.setStrokeWidth(1.0);
        
        Label crossTxt = new Label("A/C PAYEE");
        crossTxt.setFont(Font.font("Arial", FontWeight.BOLD, 7));
        crossTxt.setTextFill(Color.web("#475569", 0.9));
        crossTxt.setLayoutX(28);
        crossTxt.setLayoutY(24);
        crossTxt.setRotate(-33.7);
        
        crossing.getChildren().addAll(crossLine1, crossLine2, crossTxt);
        pane.getChildren().add(crossing);

        // 4. Branch details (top right)
        String ifsc = switch (bankCode) {
            case "SBI" -> "SBIN0001234";
            case "HDFC" -> "HDFC0000123";
            case "ICICI" -> "ICIC0000456";
            case "AXIS" -> "UTIB0000789";
            case "BOB", "BARODA" -> "BARB0000321";
            default -> "BANK0000999";
        };
        Label branchLbl = new Label("BRANCH: " + ifsc);
        branchLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
        branchLbl.setTextFill(Color.web("#475569"));
        branchLbl.setLayoutX(widthPx - 180);
        branchLbl.setLayoutY(20);
        
        javafx.scene.shape.Line branchLine = new javafx.scene.shape.Line(widthPx - 180, 32, widthPx - 30, 32);
        branchLine.setStroke(Color.web("#94a3b8"));
        branchLine.setStrokeWidth(0.8);
        pane.getChildren().addAll(branchLbl, branchLine);

        // 5. MICR bottom band background
        javafx.scene.shape.Rectangle micrBand = new javafx.scene.shape.Rectangle(0, heightPx - 40, widthPx, 40);
        micrBand.setFill(Color.web("#f8fafc"));
        micrBand.setStroke(Color.web("#cbd5e1"));
        micrBand.setStrokeWidth(0.5);
        pane.getChildren().add(micrBand);

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
                    // Draw static DATE text label
                    Label staticDate = new Label("DATE");
                    staticDate.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                    staticDate.setTextFill(Color.web("#475569"));
                    staticDate.setLayoutX(x - 35);
                    staticDate.setLayoutY(y + (h - 12) / 2);
                    pane.getChildren().add(staticDate);

                    // Draw 8 boxes (perfect squares centered vertically)
                    double boxSize = w / 8.0;
                    double by = y + (h - boxSize) / 2.0;
                    String dateStr = "";
                    if (cheque.getIssueDate() != null) {
                        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy");
                        dateStr = cheque.getIssueDate().format(dtf);
                    }

                    for (int i = 0; i < 8; i++) {
                        double bx = x + i * boxSize;
                        javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(bx, by, boxSize - 1, boxSize - 1);
                        r.setFill(Color.TRANSPARENT);
                        r.setStroke(Color.web("#475569"));
                        r.setStrokeWidth(1.0);
                        pane.getChildren().add(r);

                        if (i < dateStr.length()) {
                            char digit = dateStr.charAt(i);
                            Label dbl = new Label(String.valueOf(digit));
                            dbl.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
                            dbl.setTextFill(Color.BLACK);
                            dbl.setAlignment(Pos.CENTER);
                            dbl.setLayoutX(bx);
                            dbl.setLayoutY(by);
                            dbl.setPrefSize(boxSize - 1, boxSize - 1);
                            pane.getChildren().add(dbl);
                        }
                    }
                }
                case PAYEE -> {
                    // Draw static PAY text exactly sitting on the line baseline
                    Label staticPay = new Label("PAY");
                    staticPay.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                    staticPay.setTextFill(Color.web("#334155"));
                    staticPay.setLayoutX(x - 30);
                    staticPay.setLayoutY(y + h - 16);
                    staticPay.setPrefHeight(14);
                    staticPay.setAlignment(Pos.BOTTOM_LEFT);
                    pane.getChildren().add(staticPay);

                    // Draw payee line exactly sitting on the baseline
                    javafx.scene.shape.Line payLine = new javafx.scene.shape.Line(x, y + h, x + w, y + h);
                    payLine.setStroke(Color.web("#475569"));
                    payLine.setStrokeWidth(1.0);
                    pane.getChildren().add(payLine);

                    // Dynamic payee label sitting perfectly on the underline
                    Label payeeLbl = new Label(cheque.getPayeeName() != null ? cheque.getPayeeName() : "");
                    payeeLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                    payeeLbl.setTextFill(Color.BLACK);
                    payeeLbl.setAlignment(Pos.BOTTOM_LEFT);
                    payeeLbl.setStyle("-fx-padding: 0 0 1 5;");
                    payeeLbl.setLayoutX(x);
                    payeeLbl.setLayoutY(y + h - 16);
                    payeeLbl.setPrefSize(w, 14);
                    pane.getChildren().add(payeeLbl);
                }
                case AMOUNT_NUMBER -> {
                    // Draw static ₹ symbol
                    Label staticSymbol = new Label("₹");
                    staticSymbol.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                    staticSymbol.setTextFill(Color.web("#1e293b"));
                    staticSymbol.setLayoutX(x - 22);
                    staticSymbol.setLayoutY(y + (h - 24) / 2);
                    pane.getChildren().add(staticSymbol);

                    // Draw amount box double-border (like the image)
                    javafx.scene.shape.Rectangle outerBox = new javafx.scene.shape.Rectangle(x, y, w, h);
                    outerBox.setFill(Color.web("#fefce8", 0.4));
                    outerBox.setStroke(Color.web("#1e293b"));
                    outerBox.setStrokeWidth(1.5);

                    javafx.scene.shape.Rectangle innerBox = new javafx.scene.shape.Rectangle(x + 2, y + 2, w - 4, h - 4);
                    innerBox.setFill(Color.TRANSPARENT);
                    innerBox.setStroke(Color.web("#1e293b"));
                    innerBox.setStrokeWidth(0.5);

                    pane.getChildren().addAll(outerBox, innerBox);

                    // Dynamic amount label in monospace style
                    Label amountLbl = new Label(cheque.getAmount() != null ? cheque.getAmount().toPlainString() + "/-" : "");
                    amountLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
                    amountLbl.setTextFill(Color.BLACK);
                    amountLbl.setAlignment(Pos.CENTER);
                    amountLbl.setLayoutX(x + 3);
                    amountLbl.setLayoutY(y + 3);
                    amountLbl.setPrefSize(w - 6, h - 6);
                    pane.getChildren().add(amountLbl);
                }
                case AMOUNT_WORDS -> {
                    // Draw static RUPEES text sitting on line 1 baseline
                    Label staticRupees = new Label("RUPEES");
                    staticRupees.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                    staticRupees.setTextFill(Color.web("#334155"));
                    staticRupees.setLayoutX(x - 55);
                    staticRupees.setLayoutY(y + h/2 - 16);
                    staticRupees.setPrefHeight(14);
                    staticRupees.setAlignment(Pos.BOTTOM_LEFT);
                    pane.getChildren().add(staticRupees);

                    // Draw line 1
                    javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(x, y + h/2, x + w, y + h/2);
                    line1.setStroke(Color.web("#475569"));
                    line1.setStrokeWidth(0.8);

                    // Draw line 2 (starts under RUPEES)
                    javafx.scene.shape.Line line2 = new javafx.scene.shape.Line(x - 55, y + h, x + w, y + h);
                    line2.setStroke(Color.web("#475569"));
                    line2.setStrokeWidth(0.8);

                    pane.getChildren().addAll(line1, line2);

                    // Dynamic words split across the two baselines
                    String fullWords = cheque.getAmountWords() != null ? cheque.getAmountWords() + " Only" : "";
                    String[] splitWords = splitAmountWords(fullWords, 42);

                    Label wordsLbl1 = new Label(splitWords[0]);
                    wordsLbl1.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                    wordsLbl1.setTextFill(Color.BLACK);
                    wordsLbl1.setAlignment(Pos.BOTTOM_LEFT);
                    wordsLbl1.setStyle("-fx-padding: 0 0 1 5;");
                    wordsLbl1.setLayoutX(x);
                    wordsLbl1.setLayoutY(y + h/2 - 16);
                    wordsLbl1.setPrefSize(w, 14);
                    pane.getChildren().add(wordsLbl1);

                    Label wordsLbl2 = new Label(splitWords[1]);
                    wordsLbl2.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                    wordsLbl2.setTextFill(Color.BLACK);
                    wordsLbl2.setAlignment(Pos.BOTTOM_LEFT);
                    wordsLbl2.setStyle("-fx-padding: 0 0 1 5;");
                    wordsLbl2.setLayoutX(x - 55);
                    wordsLbl2.setLayoutY(y + h - 16);
                    wordsLbl2.setPrefSize(w + 55, 14);
                    pane.getChildren().add(wordsLbl2);
                }
                case SIGNATURE -> {
                    // Draw static signatory labels
                    Label staticFor = new Label("For " + (bank != null ? bank.getBankName().toUpperCase() : "BANK"));
                    staticFor.setFont(Font.font("Arial", FontWeight.BOLD, 8));
                    staticFor.setTextFill(Color.web("#475569"));
                    staticFor.setLayoutX(x);
                    staticFor.setLayoutY(y);
                    staticFor.setPrefWidth(w);
                    staticFor.setAlignment(Pos.CENTER);

                    Label staticAuth = new Label(":AUTHORIZED SIGNATORY");
                    staticAuth.setFont(Font.font("Arial", FontWeight.BOLD, 8));
                    staticAuth.setTextFill(Color.web("#475569"));
                    staticAuth.setLayoutX(x);
                    staticAuth.setLayoutY(y + h - 12);
                    staticAuth.setPrefWidth(w);
                    staticAuth.setAlignment(Pos.CENTER);

                    pane.getChildren().addAll(staticFor, staticAuth);

                    // Draw signature if uploaded, fallback to elegant cursive text representing the signature
                    boolean sigDrawn = false;
                    try {
                        if (SignatureService.hasSignature()) {
                            ImageView sigImage = new ImageView(new Image(SignatureService.getSignaturePath().toUri().toString()));
                            sigImage.setFitWidth(w);
                            sigImage.setFitHeight(h - 22);
                            sigImage.setPreserveRatio(true);
                            sigImage.setLayoutX(x);
                            sigImage.setLayoutY(y + 10);
                            pane.getChildren().add(sigImage);
                            sigDrawn = true;
                        }
                    } catch (Exception ignored) {}

                    if (!sigDrawn) {
                        Label curSig = new Label("Authorized Sign");
                        curSig.setFont(Font.font("Segoe Script", FontWeight.BOLD, 14));
                        curSig.setTextFill(Color.web("#1e3a8a", 0.85));
                        curSig.setLayoutX(x);
                        curSig.setLayoutY(y + 8);
                        curSig.setPrefWidth(w);
                        curSig.setAlignment(Pos.CENTER);
                        pane.getChildren().add(curSig);
                    }
                }
                case BANK_LOGO -> {
                    // Header box container for bank name and logo
                    javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox(10);
                    headerBox.setAlignment(Pos.CENTER_LEFT);
                    headerBox.setLayoutX(x);
                    headerBox.setLayoutY(y);
                    headerBox.setPrefSize(w, h);

                    javafx.scene.Node logoNode = createBankLogo(bankCode);
                    
                    Label bankLbl = new Label(bank != null ? bank.getBankName().toUpperCase() : "BANK");
                    bankLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
                    bankLbl.setTextFill(Color.web(getBankThemeColor(bankCode)));

                    headerBox.getChildren().addAll(logoNode, bankLbl);
                    pane.getChildren().add(headerBox);
                }
                case MICR -> {
                    // Draw micr numbers inside the bottom white band
                    String micrNo = cheque.getChequeNo() != null ? cheque.getChequeNo() : "000000";
                    Label micrLbl = new Label("⑆ " + micrNo + " ⑆ 000000000 ⑆ 000000 ⑆ 00");
                    micrLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
                    micrLbl.setTextFill(Color.web("#0f172a"));
                    micrLbl.setAlignment(Pos.CENTER);
                    micrLbl.setLayoutX(x);
                    micrLbl.setLayoutY(heightPx - 28);
                    micrLbl.setPrefSize(w, 20);
                    pane.getChildren().add(micrLbl);
                }
            }
        }

        // Add QR Code if generated
        try {
            String qrPath = QrCodeGenerator.generateQrCode(cheque);
            if (qrPath != null) {
                ImageView qrView = new ImageView(new Image(new File(qrPath).toURI().toString()));
                qrView.setFitWidth(35);
                qrView.setFitHeight(35);
                qrView.setLayoutX(15);
                qrView.setLayoutY(heightPx - 80);
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

    private static javafx.scene.Node createBankLogo(String bankCode) {
        javafx.scene.Group logoGroup = new javafx.scene.Group();
        switch (bankCode) {
            case "SBI" -> {
                javafx.scene.shape.Circle outer = new javafx.scene.shape.Circle(12, Color.web("#008ecb"));
                javafx.scene.shape.Circle inner = new javafx.scene.shape.Circle(3.5, Color.WHITE);
                javafx.scene.shape.Rectangle line = new javafx.scene.shape.Rectangle(-1.5, 2, 3, 10);
                line.setFill(Color.WHITE);
                logoGroup.getChildren().addAll(outer, inner, line);
            }
            case "BOB", "BARODA" -> {
                javafx.scene.shape.Arc arc1 = new javafx.scene.shape.Arc(0, 0, 12, 12, -30, 240);
                arc1.setType(javafx.scene.shape.ArcType.ROUND);
                arc1.setFill(Color.web("#f05a28"));
                javafx.scene.shape.Arc arc2 = new javafx.scene.shape.Arc(3, 0, 8, 8, -30, 240);
                arc2.setType(javafx.scene.shape.ArcType.ROUND);
                arc2.setFill(Color.WHITE);
                logoGroup.getChildren().addAll(arc1, arc2);
            }
            case "HDFC" -> {
                javafx.scene.shape.Rectangle box = new javafx.scene.shape.Rectangle(-12, -12, 24, 24);
                box.setFill(Color.web("#003366"));
                box.setArcWidth(4);
                box.setArcHeight(4);
                javafx.scene.shape.Circle center = new javafx.scene.shape.Circle(4, Color.WHITE);
                logoGroup.getChildren().addAll(box, center);
            }
            case "ICICI" -> {
                javafx.scene.shape.Circle outer = new javafx.scene.shape.Circle(12, Color.web("#bc2c3d"));
                javafx.scene.text.Text txt = new javafx.scene.text.Text(-3, 5, "i");
                txt.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
                txt.setFill(Color.web("#ffb81c"));
                logoGroup.getChildren().addAll(outer, txt);
            }
            default -> {
                javafx.scene.shape.Polygon roof = new javafx.scene.shape.Polygon(
                    0, -10, -12, -3, 12, -3
                );
                roof.setFill(Color.web("#1e3a8a"));
                javafx.scene.shape.Rectangle base = new javafx.scene.shape.Rectangle(-12, 7, 24, 3);
                base.setFill(Color.web("#1e3a8a"));
                javafx.scene.shape.Rectangle col1 = new javafx.scene.shape.Rectangle(-9, -3, 3, 10);
                col1.setFill(Color.web("#1e3a8a"));
                javafx.scene.shape.Rectangle col2 = new javafx.scene.shape.Rectangle(-1.5, -3, 3, 10);
                col2.setFill(Color.web("#1e3a8a"));
                javafx.scene.shape.Rectangle col3 = new javafx.scene.shape.Rectangle(6, -3, 3, 10);
                col3.setFill(Color.web("#1e3a8a"));
                logoGroup.getChildren().addAll(roof, base, col1, col2, col3);
            }
        }
        return logoGroup;
    }

    private static String getBankThemeColor(String bankCode) {
        return switch (bankCode) {
            case "SBI" -> "#008ecb";
            case "HDFC" -> "#003366";
            case "ICICI" -> "#bc2c3d";
            case "AXIS" -> "#7b113a";
            case "BOB", "BARODA" -> "#f05a28";
            default -> "#1e3a8a";
        };
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

    private static String[] splitAmountWords(String fullWords, int maxCharsLine1) {
        if (fullWords == null || fullWords.isBlank()) {
            return new String[]{"", ""};
        }
        fullWords = fullWords.trim();
        if (fullWords.length() <= maxCharsLine1) {
            return new String[]{fullWords, ""};
        }
        int splitIdx = fullWords.lastIndexOf(' ', maxCharsLine1);
        if (splitIdx == -1) {
            splitIdx = maxCharsLine1;
        }
        String w1 = fullWords.substring(0, splitIdx).trim();
        String w2 = fullWords.substring(splitIdx).trim();
        return new String[]{w1, w2};
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
