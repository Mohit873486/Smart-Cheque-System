package com.chequeprint.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * FxUtils — reusable JavaFX animation and UI helper methods.
 *
 * FIX in switchPage():
 *   Previously, in.setOpacity(0) was called AFTER onSwitch.run() had already
 *   added 'in' to the scene, causing a single-frame flash at full opacity
 *   before the fade-in started. The fix moves in.setOpacity(0) to BEFORE
 *   onSwitch.run() so the node enters the scene already invisible.
 *
 *   NOTE: MainController.navigate() no longer calls switchPage() directly —
 *   it inlines the animation to avoid the detached-node/setOnFinished bug.
 *   switchPage() is kept here for any other callers that may use it.
 */
public class FxUtils {

    /** Fade + slide-up entrance animation for any node. */
    public static void animateIn(Node node, double delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);

        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(380), node);
        slide.setFromY(18);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Scale-bounce animation for stat cards on load. */
    public static void bounceIn(Node node, double delayMs) {
        node.setScaleX(0.88);
        node.setScaleY(0.88);
        node.setOpacity(0);

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

    /** Animated counter for Label — counts from 0 to target. */
    public static void countUp(Label label, int target, String prefix, double delayMs) {
        Timeline tl = new Timeline();
        long duration = 900;
        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            final int val = (int) Math.round(target * easeOut((double) i / steps));
            KeyFrame kf = new KeyFrame(
                Duration.millis(delayMs + (duration * i / steps)),
                e -> label.setText(prefix + String.format("%,d", val))
            );
            tl.getKeyFrames().add(kf);
        }
        tl.play();
    }

    /** Ease-out cubic for countUp. */
    private static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    /** Hover pulse — brief scale-up on mouse enter. */
    public static void addHoverEffect(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
            st.setToX(1.03); st.setToY(1.03);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
    }

    /** Shake animation — use on validation errors. */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0); tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /**
     * Page-switch: fade out old node, then swap via onSwitch callback, then fade in new node.
     *
     * FIX: in.setOpacity(0) is now called BEFORE onSwitch.run() so the incoming
     * node is already invisible when it enters the scene — no flash.
     *
     * IMPORTANT: The caller must ensure 'in' is not yet in the scene graph when
     * this method is called. The swap (adding 'in' / removing 'out') happens
     * inside onSwitch, which runs only after the fade-out completes, so 'out'
     * is no longer being animated when it is detached — making it safe.
     */
    public static void switchPage(Node out, Node in, Runnable onSwitch) {
        // FIX: set incoming node invisible BEFORE it enters the scene
        in.setOpacity(0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), out);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            // 'out' animation is fully complete — safe to detach it now
            if (onSwitch != null) onSwitch.run();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), in);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }
}