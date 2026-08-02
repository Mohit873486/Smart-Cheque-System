package com.chequeprint.dev;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Modern floating UI toast banner that displays hot-reload feedback
 * in the top-right corner of the application window.
 */
public class HotReloadBanner {

    private static final String BANNER_ID = "__dev_hot_reload_banner__";

    public static void show(Scene scene, String message, boolean isCss) {
        if (scene == null) return;

        Platform.runLater(() -> {
            try {
                if (!(scene.getRoot() instanceof Pane rootPane)) {
                    return;
                }

                // Remove existing banner if present
                rootPane.getChildren().removeIf(node -> BANNER_ID.equals(node.getId()));

                Label label = new Label(message);
                String bgColor = isCss ? "#059669" : "#2563eb"; // Emerald green for CSS, Royal blue for FXML
                label.setStyle(
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-family: 'Segoe UI', sans-serif; " +
                    "-fx-padding: 6px 14px; " +
                    "-fx-background-color: " + bgColor + "; " +
                    "-fx-background-radius: 20px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 8, 0, 0, 3);"
                );

                HBox bannerContainer = new HBox(label);
                bannerContainer.setId(BANNER_ID);
                bannerContainer.setAlignment(Pos.TOP_RIGHT);
                bannerContainer.setPickOnBounds(false);
                bannerContainer.setStyle("-fx-padding: 16px 24px 0 0;");
                bannerContainer.setMouseTransparent(true);

                bannerContainer.setOpacity(0);
                rootPane.getChildren().add(bannerContainer);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), bannerContainer);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                PauseTransition delay = new PauseTransition(Duration.millis(1800));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), bannerContainer);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> rootPane.getChildren().remove(bannerContainer));

                SequentialTransition seq = new SequentialTransition(fadeIn, delay, fadeOut);
                seq.play();
            } catch (Exception e) {
                System.err.println("[HotReloadBanner] Banner display warning: " + e.getMessage());
            }
        });
    }
}
