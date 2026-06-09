package com.chequeprint;

import com.chequeprint.config.AppConfig;
import com.chequeprint.service.ChequeReminderScheduler;
import com.chequeprint.util.DBConnection;
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
 *
 * Fixes applied vs original:
 *  • Loads login.fxml FIRST (not main.fxml directly) so users authenticate.
 *  • Removed broken @FXML minimizeWindow() — it had no FXML scene reference.
 *  • Added Application.stop() to close DB connection on exit.
 *  • Removed accidental stage.setIconified(true) that minimised window on launch.
 *  • Splash runs DB check on background thread to avoid blocking the UI thread.
 */
public class MainApp extends Application {

    private final ChequeReminderScheduler reminderScheduler = new ChequeReminderScheduler();

    @Override
    public void start(Stage primaryStage) {
        reminderScheduler.startDaily();
        showSplash(primaryStage);
    }

    // ── Clean shutdown ───────────────────────────────────────────────
    @Override
    public void stop() {
        reminderScheduler.stop();
        AppConfig.closeConnection();
        DBConnection.closeConnection();
    }

    // ── Splash Screen ────────────────────────────────────────────────
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

        // DB check runs on background thread; UI updates back on FX thread
        Timeline splash = new Timeline(
            new KeyFrame(Duration.millis(0), e -> {
                pb.setProgress(0.1);
                loadingText.setText("Starting…");
            }),
            new KeyFrame(Duration.millis(500), e -> {
                pb.setProgress(0.35);
                loadingText.setText("Checking database…");
                // Background DB ping & migration
                new Thread(() -> {
                    boolean ok = AppConfig.isConnected();
                    if (ok) {
                        try (java.sql.Connection conn = AppConfig.getConnection();
                             java.sql.Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("ALTER TABLE cheques MODIFY COLUMN status "
                                + "ENUM('Draft','Pending','Approved','Rejected','Printed','Cancelled','Deposited','Cleared','Bounced') "
                                + "NOT NULL DEFAULT 'Draft'");
                            System.out.println("✓ Database migration: status enum updated successfully.");
                        } catch (Exception ex) {
                            System.err.println("Migration warning: " + ex.getMessage());
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            pb.setProgress(0.65);
                            loadingText.setText("Database connected ✓");
                        } else {
                            loadingText.setText("⚠ DB offline — running in limited mode");
                            pb.setProgress(0.65);
                        }
                    });
                }, "splash-db-check").start();
            }),
            new KeyFrame(Duration.millis(1400), e -> {
                pb.setProgress(0.9);
                loadingText.setText("Loading UI…");
            }),
            new KeyFrame(Duration.millis(1900), e -> {
                pb.setProgress(1.0);
            })
        );

        splash.setOnFinished(e -> {
            splashStage.close();
            loadLoginScreen(primaryStage);
        });

        splash.play();
    }

    // ── Load Login Screen ────────────────────────────────────────────
    /**
     * Shows login.fxml first. After successful authentication, LoginController
     * swaps the scene to main.fxml itself.
     */
    private void loadLoginScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 680);
            scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("ChequePro — Sign In");
            stage.setMinWidth(480);
            stage.setMinHeight(560);
            // Centre on screen
            stage.centerOnScreen();
            stage.show();

            // Fade in
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
