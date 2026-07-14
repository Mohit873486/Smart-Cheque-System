package com.chequeprint.util;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final Preferences PREFS = Preferences.userRoot().node("com/chequeprint/theme");

    /** Save the theme to local storage (equivalent to localStorage.setItem) */
    public static void saveTheme(String theme) {
        PREFS.put("active_theme", theme != null ? theme.toLowerCase() : "light");
    }

    /** Retrieve the saved theme from local storage (equivalent to localStorage.getItem) */
    public static String getSavedTheme() {
        return PREFS.get("active_theme", "light");
    }

    /** Apply theme to scene and persist choice */
    public static void applyTheme(Scene scene, String theme) {
        if (scene == null) return;
        
        saveTheme(theme);
        
        System.out.println("[ThemeManager] Applying theme: " + theme);
        System.out.println("[ThemeManager] Stylesheets before: " + new java.util.ArrayList<>(scene.getStylesheets()));
        
        // Remove existing theme overrides
        scene.getStylesheets().removeIf(s -> s.contains("dark.css") || s.contains("dark"));
        
        // Add dark theme override if active
        if ("dark".equalsIgnoreCase(theme)) {
            var darkCss = ThemeManager.class.getResource("/css/dark.css");
            if (darkCss != null) {
                scene.getStylesheets().add(darkCss.toExternalForm());
            } else {
                System.err.println("[ThemeManager] dark.css resource not found!");
            }
        }
        
        System.out.println("[ThemeManager] Stylesheets after: " + new java.util.ArrayList<>(scene.getStylesheets()));
    }

    /** Load and apply the saved theme on scene load */
    public static void applySavedTheme(Scene scene) {
        applyTheme(scene, getSavedTheme());
    }
}
