# ⚡ Fast Developer Workflow & Hot Reload Guide

This guide describes the complete zero-restart developer setup for the **Smart Cheque System** (Spring Boot + JavaFX).

---

## 🚀 Key Features

1. **Spring Boot DevTools Auto-Restart**:
   - Backend automatically restarts context when server classes change.
   - Restarts in ~1-2 seconds with Hikari pool optimization.
2. **JavaFX Live Hot-Reload (FXML & CSS)**:
   - Modifying `.fxml` files in `src/main/resources/view/` instantly reloads the active screen graph.
   - Modifying `.css` files in `src/main/resources/css/` instantly refreshes styles in **10-30ms** without destroying UI state or screen elements.
   - Preserves controller state (search text, active view, form inputs) via `ReloadableController` interface.
3. **Visual Toast Feedback**:
   - Displays real-time reload indicators (e.g. `⚡ View Hot-Reloaded (45ms)` or `🎨 Stylesheet Refreshed (12ms)`).
4. **Resilient Backend API Reconnect**:
   - `ApiClient` automatically retries connection drops with exponential backoff while Spring Boot DevTools restarts, avoiding abrupt app crashes.

---

## 🛠️ Step-by-Step Developer Setup

### 1. Launching Backend (Spring Boot DevTools)

In terminal window 1:
```powershell
mvn spring-boot:run -f server/pom.xml
```
- DevTools is enabled in `server/src/main/resources/application.properties`.
- When you edit any `.java` class in `server/src/main/java`, saving the file will automatically trigger an application context restart within 1-2 seconds. No manual `mvn` restart required!

---

### 2. Launching Frontend (JavaFX)

In terminal window 2:
```powershell
mvn javafx:run -f pom.xml
```
- The `DevHotReloadService` automatically starts in development mode.
- Edit any FXML (`src/main/resources/view/*.fxml`) or CSS (`src/main/resources/css/*.css`) in your editor (VS Code, IntelliJ, Eclipse).
- Upon saving, the changes immediately reflect on the live running JavaFX window without closing or restarting the application!

---

## 💡 IDE Auto-Build Setup (Instant Save-to-Reload)

To ensure saving files immediately triggers recompilation/reload:

### VS Code
1. Open Settings (`Ctrl + ,`).
2. Search for `Java: Auto Build`.
3. Ensure `"java.autobuild.enabled": true` is checked.

### IntelliJ IDEA
1. Open **Settings** (`Ctrl + Alt + S`) → **Build, Execution, Deployment** → **Compiler**.
2. Check **Build project automatically**.
3. Go to **Advanced Settings** → Check **Allow auto-make to start even if developed application is currently running**.

---

## 🔮 Bonus: DCEVM & HotswapAgent (Advanced Java Hot Swap)

If you want **instant Java code hot swapping** (adding new methods, changing method bodies, adding fields without restarting JVM or Spring Boot context):

### What is DCEVM?
DCEVM (Dynamic Code Evolution VM) is an enhanced JVM patch that allows unlimited class redefinition at runtime.

### How to Setup DCEVM + HotswapAgent:
1. Download **JetBrains Runtime (JBR)** or **Trava OpenJDK** (which includes DCEVM built-in) for Java 17.
2. Add `-XX:+AllowEnhancedClassRedefinition -XX:HotswapAgent=fat` to JVM arguments in Maven or IDE run config.
3. With HotswapAgent active, editing Java controller/service classes updates the running JVM in **milliseconds** without Spring Boot or JavaFX app restarts.

---

## 🛡️ Summary of Architecture

```
 ┌────────────────────────┐         ┌────────────────────────┐
 │   Spring Boot Backend  │         │    JavaFX Frontend     │
 │  (Port 8081 - DevTools)│◄────────┤    (MainApp / UI)      │
 └───────────▲────────────┘  HTTP   └───────────▲────────────┘
             │  Auto Restart                    │  NIO Watcher
             │  (Target Class)                  │  (FXML / CSS)
     ┌───────┴───────┐                  ┌───────┴───────┐
     │ Server Code   │                  │ FXML & CSS    │
     │ Modifications │                  │ Modifications │
     └───────────────┘                  └───────────────┘
```
