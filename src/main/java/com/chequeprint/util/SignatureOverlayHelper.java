package com.chequeprint.util;

import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.Locale;
import java.util.Properties;

/**
 * Helper for placing signature image on a cheque Pane and syncing X/Y/size with text fields.
 */
public final class SignatureOverlayHelper {

    private SignatureOverlayHelper() {
    }

    public static ImageView attachSignature(Pane chequePane, TextField tfX, TextField tfY, TextField tfW, TextField tfH) {
        if (chequePane == null) {
            throw new IllegalArgumentException("chequePane is required");
        }

        Image signatureImage = SignatureService.loadSignatureImage();
        if (signatureImage == null) {
            return null;
        }

        ImageView signature = new ImageView(signatureImage);
        signature.setPreserveRatio(true);
        signature.setSmooth(true);
        signature.setManaged(false);

        Properties meta = SignatureService.loadMetadata();
        double x = parseDouble(meta.getProperty("x", "0"), 0);
        double y = parseDouble(meta.getProperty("y", "0"), 0);
        double w = parsePx(meta.getProperty("width", "120px"), 120);
        double h = parsePx(meta.getProperty("height", "40px"), 40);

        signature.setFitWidth(w);
        signature.setFitHeight(h);
        signature.setLayoutX(x);
        signature.setLayoutY(y);

        if (tfX != null) tfX.setText(fmt(x));
        if (tfY != null) tfY.setText(fmt(y));
        if (tfW != null) tfW.setText(fmt(w));
        if (tfH != null) tfH.setText(fmt(h));

        installDragHandler(signature, tfX, tfY);
        bindFieldUpdates(signature, tfX, tfY, tfW, tfH);

        chequePane.getChildren().add(signature);
        return signature;
    }

    public static void persistSignatureState(ImageView signature, TextField tfX, TextField tfY, TextField tfW, TextField tfH) {
        if (signature == null) {
            return;
        }
        Properties meta = SignatureService.loadMetadata();
        meta.setProperty("x", tfX != null ? tfX.getText().trim() : fmt(signature.getLayoutX()));
        meta.setProperty("y", tfY != null ? tfY.getText().trim() : fmt(signature.getLayoutY()));
        meta.setProperty("width", (tfW != null ? tfW.getText().trim() : fmt(signature.getFitWidth())) + "px");
        meta.setProperty("height", (tfH != null ? tfH.getText().trim() : fmt(signature.getFitHeight())) + "px");
        SignatureService.saveMetadata(meta);
    }

    private static void bindFieldUpdates(ImageView signature, TextField tfX, TextField tfY, TextField tfW, TextField tfH) {
        if (tfX != null) {
            tfX.textProperty().addListener((obs, oldVal, newVal) -> signature.setLayoutX(parseDouble(newVal, signature.getLayoutX())));
        }
        if (tfY != null) {
            tfY.textProperty().addListener((obs, oldVal, newVal) -> signature.setLayoutY(parseDouble(newVal, signature.getLayoutY())));
        }
        if (tfW != null) {
            tfW.textProperty().addListener((obs, oldVal, newVal) -> signature.setFitWidth(parsePx(newVal, signature.getFitWidth())));
        }
        if (tfH != null) {
            tfH.textProperty().addListener((obs, oldVal, newVal) -> signature.setFitHeight(parsePx(newVal, signature.getFitHeight())));
        }
    }

    private static void installDragHandler(ImageView signature, TextField tfX, TextField tfY) {
        final Delta delta = new Delta();

        signature.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            delta.x = e.getX();
            delta.y = e.getY();
        });

        signature.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            double x = signature.getLayoutX() + (e.getX() - delta.x);
            double y = signature.getLayoutY() + (e.getY() - delta.y);
            signature.setLayoutX(x);
            signature.setLayoutY(y);
            if (tfX != null) tfX.setText(fmt(x));
            if (tfY != null) tfY.setText(fmt(y));
        });
    }

    private static double parsePx(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.toLowerCase(Locale.ROOT).replace("px", "").trim();
        return parseDouble(cleaned, fallback);
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value == null ? "" : value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static final class Delta {
        private double x;
        private double y;
    }
}
