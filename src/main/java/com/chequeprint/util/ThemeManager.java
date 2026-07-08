package com.chequeprint.util;

import javafx.scene.Scene;

public class ThemeManager {

    public static void applyTheme(Scene scene, String theme) {
        if (scene == null) return;
        
        // Remove existing dark theme override if present
        scene.getStylesheets().removeIf(s -> s.contains("dark.css"));
        
        // Apply dark theme if requested
        if ("dark".equalsIgnoreCase(theme)) {
            var darkCss = ThemeManager.class.getResource("/css/dark.css");
            if (darkCss != null) {
                scene.getStylesheets().add(darkCss.toExternalForm());
            } else {
                System.err.println("[ThemeManager] dark.css stylesheet resource not found!");
            }
        }
    }
}
