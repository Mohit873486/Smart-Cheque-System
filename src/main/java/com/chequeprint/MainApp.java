package com.chequeprint;

import com.chequeprint.service.ChequeReminderScheduler;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * MainApp — application entry point.
 */
public class MainApp extends Application {

    private final ChequeReminderScheduler reminderScheduler = new ChequeReminderScheduler();

    @Override
    public void start(Stage primaryStage) {
        com.chequeprint.util.GlobalErrorHandler.init();
        reminderScheduler.startDaily();
        showSplash(primaryStage);
    }

    @Override
    public void stop() {
        // Client has no direct DB connections — all access via REST API
        System.out.println("Application stopped gracefully.");
    }

    private void showSplash(Stage primaryStage) {
        Label logo = new Label("💼 ChequePro");
        logo.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:#1a56db;");

        Label tagline = new Label("Professional Cheque & Invoice Manager");
        tagline.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;");

        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(260);
        pb.setStyle("-fx-accent:#1a56db;");

        Label loadingText = new Label("Initialising…");
        loadingText.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        VBox splashRoot = new VBox(12, logo, tagline, pb, loadingText);
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setStyle(
                "-fx-background-color:#ffffff;"
                        + "-fx-padding:60 80 60 80;"
                        + "-fx-background-radius:16px;"
                        + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.18),30,0,0,8);");

        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        Scene splashScene = new Scene(splashRoot);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        Timeline splash = new Timeline(
                new KeyFrame(Duration.millis(0), e -> {
                    pb.setProgress(0.1);
                    loadingText.setText("Starting…");
                }),
                new KeyFrame(Duration.millis(500), e -> {
                    pb.setProgress(0.35);
                    loadingText.setText("Checking backend…");
                    new Thread(() -> {
                        boolean ok = checkBackendConnection();
                        javafx.application.Platform.runLater(() -> {
                            if (ok) {
                                pb.setProgress(0.65);
                                loadingText.setText("Backend connected ✓");
                            } else {
                                loadingText.setText("⚠ Backend offline — running in limited mode");
                                pb.setProgress(0.65);
                            }
                        });
                    }, "splash-backend-check").start();
                }),
                new KeyFrame(Duration.millis(1400), e -> {
                    pb.setProgress(0.9);
                    loadingText.setText("Loading UI…");
                }),
                new KeyFrame(Duration.millis(1900), e -> {
                    pb.setProgress(1.0);
                }));

        splash.setOnFinished(e -> {
            splashStage.close();
            loadLoginScreen(primaryStage);
        });

        splash.play();
    }

    private boolean checkBackendConnection() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(com.chequeprint.config.ApiConfig.BASE_URL + "/api/cheques"))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadLoginScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 680);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            com.chequeprint.util.ThemeManager.applySavedTheme(scene);

            stage.setScene(scene);
            stage.setTitle("ChequePro — Sign In");
            stage.setMinWidth(480);
            stage.setMinHeight(560);
            stage.centerOnScreen();
            stage.show();

            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}