package com.chequeprint.util;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 * Utility helper for making JavaFX Label fields draggable on a Pane.
 * Uses setOnMousePressed() and setOnMouseDragged() to update X and Y positions in real time.
 */
public class DraggableLabelUtil {

    private static class DragContext {
        double mouseAnchorX;
        double mouseAnchorY;
    }

    private DraggableLabelUtil() {
        // Utility class
    }

    /**
     * Attaches mouse drag handlers to a Label node.
     *
     * @param label      The Label node to make draggable
     * @param parentPane The parent Pane container bounds
     * @param onPositionChange Optional listener callback fired in real-time when X/Y change
     */
    public static void makeDraggable(Label label, Pane parentPane, PositionChangeListener onPositionChange) {
        if (label == null || parentPane == null) {
            return;
        }

        final DragContext dragContext = new DragContext();

        // 1. Store mouse anchor point on press
        label.setOnMousePressed(event -> {
            dragContext.mouseAnchorX = event.getX();
            dragContext.mouseAnchorY = event.getY();
            label.toFront();
        });

        // 2. Update position in real time on mouse drag using setOnMouseDragged()
        label.setOnMouseDragged(event -> {
            double newX = label.getLayoutX() + (event.getX() - dragContext.mouseAnchorX);
            double newY = label.getLayoutY() + (event.getY() - dragContext.mouseAnchorY);

            // Clamp bounds inside parent Pane
            double maxX = Math.max(0, parentPane.getWidth() - label.getWidth());
            double maxY = Math.max(0, parentPane.getHeight() - label.getHeight());

            newX = Math.max(0, Math.min(newX, maxX > 0 ? maxX : parentPane.getPrefWidth() - label.getWidth()));
            newY = Math.max(0, Math.min(newY, maxY > 0 ? maxY : parentPane.getPrefHeight() - label.getHeight()));

            // Update layout position
            label.setLayoutX(newX);
            label.setLayoutY(newY);

            // Trigger real-time position listener
            if (onPositionChange != null) {
                onPositionChange.onPositionChanged(newX, newY);
            }
        });
    }

    @FunctionalInterface
    public interface PositionChangeListener {
        void onPositionChanged(double x, double y);
    }
}
