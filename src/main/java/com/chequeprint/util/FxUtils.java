package com.chequeprint.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * FxUtils — reusable JavaFX animation and UI helpers.
 *
 * FIX in switchPage():
 *   in.setOpacity(0) is now called BEFORE onSwitch.run() so the incoming node
 *   is already invisible when added to the scene — eliminates a 1-frame flash.
 *   Null-guard added for onSwitch so callers can pass null when no swap logic
 *   is needed.
 */
public class FxUtils {

    /** Fade + slide-up entrance for any node. */
    public static void animateIn(Node node, double delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);

        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(380), node);
        slide.setFromY(18); slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Scale-bounce for stat cards on load. */
    public static void bounceIn(Node node, double delayMs) {
        node.setScaleX(0.88); node.setScaleY(0.88); node.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(420), node);
        scale.setFromX(0.88); scale.setToX(1.0);
        scale.setFromY(0.88); scale.setToY(1.0);
        scale.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));

        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setFromValue(0); fade.setToValue(1);

        ParallelTransition pt = new ParallelTransition(scale, fade);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Animated counter: counts from 0 up to target, with optional prefix. */
    public static void countUp(Label label, int target, String prefix, double delayMs) {
        Timeline tl = new Timeline();
        long duration = 900;
        int  steps    = 40;
        for (int i = 0; i <= steps; i++) {
            final int val = (int) Math.round(target * easeOut((double) i / steps));
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(delayMs + (duration * i / steps)),
                e -> label.setText(prefix + String.format("%,d", val))
            ));
        }
        tl.play();
    }

    private static double easeOut(double t) { return 1 - Math.pow(1 - t, 3); }

    /** Hover pulse on mouse enter/exit. */
    public static void addHoverEffect(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
    }

    /** Shake animation for validation errors. */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0); tt.setByX(10);
        tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /**
     * Page-switch utility: fade out 'out', run onSwitch (to swap nodes), then fade in 'in'.
     *
     * FIX: in.setOpacity(0) now runs BEFORE onSwitch.run() so 'in' enters the
     * scene already invisible — no single-frame flash.
     *
     * NOTE: MainController.navigate() no longer uses this method because it
     * inlines the animation to avoid the detached-node/setOnFinished bug.
     * This helper remains for other call sites.
     *
     * @param out      the node currently visible (will be faded out)
     * @param in       the node to show (must not yet be in the scene)
     * @param onSwitch callback that performs the actual scene-graph swap; may be null
     */
    public static void switchPage(Node out, Node in, Runnable onSwitch) {
        in.setOpacity(0);   // FIX: invisible before entering the scene

        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), out);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            if (onSwitch != null) onSwitch.run();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), in);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }
}
