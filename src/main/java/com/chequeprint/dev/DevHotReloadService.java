package com.chequeprint.dev;

import com.chequeprint.controller.MainController;
import javafx.application.Platform;
import javafx.scene.Scene;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DevHotReloadService — NIO File Watcher service for JavaFX dynamic hot-reloading.
 * Watches FXML and CSS files for modifications and triggers instant view/style reloads.
 */
public class DevHotReloadService {

    private static DevHotReloadService instance;
    private WatchService watchService;
    private Thread watcherThread;
    private MainController mainController;
    private volatile boolean running = false;

    // Debounce watcher events (300ms debounce)
    private final Map<String, Long> lastTriggerMap = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MS = 300;

    private DevHotReloadService() {}

    public static synchronized DevHotReloadService getInstance() {
        if (instance == null) {
            instance = new DevHotReloadService();
        }
        return instance;
    }

    public void start(MainController mainController) {
        this.mainController = mainController;
        if (running) return;

        List<Path> watchPaths = findWatchDirectories();
        if (watchPaths.isEmpty()) {
            System.out.println("[DevHotReloadService] No dev resource directories found. Hot reload disabled.");
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            for (Path path : watchPaths) {
                if (Files.exists(path) && Files.isDirectory(path)) {
                    path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
                    System.out.println("[DevHotReloadService] Watching for live changes in: " + path.toAbsolutePath());
                }
            }

            running = true;
            watcherThread = new Thread(this::watchLoop, "dev-hot-reload-watcher");
            watcherThread.setDaemon(true);
            watcherThread.start();
            System.out.println("⚡ [DevHotReloadService] Hot Reload Service started successfully.");
        } catch (Exception e) {
            System.err.println("[DevHotReloadService] Failed to start watch service: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception ignored) {}
        }
    }

    private List<Path> findWatchDirectories() {
        List<Path> paths = new ArrayList<>();
        String userDir = System.getProperty("user.dir");

        // Primary: src/main/resources/view & css
        Path srcView = Paths.get(userDir, "src", "main", "resources", "view");
        Path srcCss = Paths.get(userDir, "src", "main", "resources", "css");

        if (Files.exists(srcView)) paths.add(srcView);
        if (Files.exists(srcCss)) paths.add(srcCss);

        // Secondary: target/classes/view & css
        Path targetView = Paths.get(userDir, "target", "classes", "view");
        Path targetCss = Paths.get(userDir, "target", "classes", "css");

        if (Files.exists(targetView)) paths.add(targetView);
        if (Files.exists(targetCss)) paths.add(targetCss);

        return paths;
    }

    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    String nameStr = filename.toString();

                    long now = System.currentTimeMillis();
                    Long lastTrigger = lastTriggerMap.get(nameStr);
                    if (lastTrigger != null && (now - lastTrigger) < DEBOUNCE_MS) {
                        continue;
                    }
                    lastTriggerMap.put(nameStr, now);

                    if (nameStr.endsWith(".css")) {
                        handleCssChange(nameStr);
                    } else if (nameStr.endsWith(".fxml")) {
                        handleFxmlChange(nameStr);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (ClosedWatchServiceException e) {
                break;
            } catch (Exception e) {
                System.err.println("[DevHotReloadService] Error in watch loop: " + e.getMessage());
            }
        }
    }

    private void handleCssChange(String cssFileName) {
        long start = System.currentTimeMillis();
        Platform.runLater(() -> {
            try {
                if (mainController == null) return;
                Scene scene = mainController.getScene();
                if (scene == null) return;

                List<String> stylesheets = new ArrayList<>(scene.getStylesheets());
                scene.getStylesheets().clear();

                for (String ss : stylesheets) {
                    // Cache buster for CSS reload
                    String base = ss.contains("?") ? ss.substring(0, ss.indexOf('?')) : ss;
                    scene.getStylesheets().add(base + "?t=" + System.currentTimeMillis());
                }

                long elapsed = System.currentTimeMillis() - start;
                System.out.println("🎨 [HotReload] Refreshed CSS stylesheet '" + cssFileName + "' in " + elapsed + "ms.");
                HotReloadBanner.show(scene, "🎨 Stylesheet Refreshed (" + elapsed + "ms)", true);
            } catch (Exception e) {
                System.err.println("[HotReload] Error reloading CSS: " + e.getMessage());
            }
        });
    }

    private void handleFxmlChange(String fxmlFileName) {
        long start = System.currentTimeMillis();
        Platform.runLater(() -> {
            try {
                if (mainController == null) return;
                mainController.reloadCurrentView(fxmlFileName);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("⚡ [HotReload] Hot-reloaded FXML view '" + fxmlFileName + "' in " + elapsed + "ms.");
                HotReloadBanner.show(mainController.getScene(), "⚡ View Hot-Reloaded (" + elapsed + "ms)", false);
            } catch (Exception e) {
                System.err.println("[HotReload] Error reloading FXML: " + e.getMessage());
            }
        });
    }
}
