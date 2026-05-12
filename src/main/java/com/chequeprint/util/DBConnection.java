package com.chequeprint.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — lightweight JDBC connection wrapper.
 *
 * Design notes:
 *  • AppConfig.java holds the primary singleton connection used by all DAO classes.
 *  • DBConnection provides a secondary, thread-safe helper for background threads
 *    that need their own connection (e.g. async loaders) so they don't share the
 *    single AppConfig connection and risk "ResultSet already closed" errors.
 *  • Call DBConnection.getConnection() instead of AppConfig.getConnection() inside
 *    any Thread that runs concurrently with the JavaFX Application Thread.
 */
public final class DBConnection {

    // ── Connection parameters — kept in sync with AppConfig ──────────
    private static final String URL  = "jdbc:mysql://localhost:3306/chequeprint_db"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Kolkata"
            + "&autoReconnect=true";

    private static final String USER = "root";
    private static final String PASS = "root123";

    // ── Thread-local so each background thread gets its own connection ─
    private static final ThreadLocal<Connection> threadLocal = new ThreadLocal<>();

    private DBConnection() {}

    /**
     * Returns a connection for the current thread.
     * Creates a new one if none exists or if the existing one is closed.
     */
    public static Connection getConnection() throws SQLException {
        Connection con = threadLocal.get();
        if (con == null || con.isClosed()) {
            con = DriverManager.getConnection(URL, USER, PASS);
            threadLocal.set(con);
        }
        return con;
    }

    /**
     * Closes and removes the connection for the current thread.
     * Call this at the end of each background task / thread lifecycle.
     */
    public static void closeConnection() {
        Connection con = threadLocal.get();
        if (con != null) {
            try { con.close(); } catch (SQLException ignored) {}
            threadLocal.remove();
        }
    }

    /**
     * Quick connectivity test — returns true when the DB is reachable.
     * Safe to call from any thread; always opens and closes its own connection.
     */
    public static boolean isReachable() {
        try (Connection test = DriverManager.getConnection(URL, USER, PASS)) {
            return test != null && !test.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns a brief status string suitable for UI display.
     * e.g. "🟢 Connected" or "🔴 Offline: Communications link failure"
     */
    public static String statusLabel() {
        try (Connection test = DriverManager.getConnection(URL, USER, PASS)) {
            return "🟢 Database connected";
        } catch (SQLException e) {
            String msg = e.getMessage();
            // Trim verbose JDBC messages for display
            if (msg != null && msg.length() > 80) msg = msg.substring(0, 80) + "…";
            return "🔴 DB offline: " + msg;
        }
    }
}