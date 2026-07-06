package com.chequeprint.util;

import com.chequeprint.model.Cheque;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class QrCodeGenerator {

    public static String generateQrCode(Cheque cheque) throws Exception {
        if (cheque == null) {
            return null;
        }

        // Build verification text
        StringBuilder sb = new StringBuilder();
        sb.append("ChequePro Verification\n");
        sb.append("Cheque No: ").append(cheque.getChequeNo()).append("\n");
        sb.append("Payee: ").append(cheque.getPayeeName()).append("\n");
        sb.append("Amount: INR ").append(cheque.getAmount() != null ? cheque.getAmount().toPlainString() : "0.00").append("\n");
        sb.append("Date: ").append(cheque.getIssueDate() != null ? cheque.getIssueDate().toString() : "").append("\n");
        sb.append("Status: ").append(cheque.getStatus() != null ? cheque.getStatus().name() : "Draft");

        String text = sb.toString();
        int width = 150;
        int height = 150;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        // Save to a temporary file
        Path tempPath = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), "qr_" + cheque.getChequeNo() + ".png");
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", tempPath);

        return tempPath.toAbsolutePath().toString();
    }
}
