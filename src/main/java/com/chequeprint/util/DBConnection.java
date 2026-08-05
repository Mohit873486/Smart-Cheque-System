package com.chequeprint.util;

import com.chequeprint.config.AppConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnection — thin helper that delegates to the shared HikariCP pool in
 * {@link AppConfig}.
 *
 * Design notes:
 *  • Previously this class maintained its own ThreadLocal of raw
 *    DriverManager connections. Now that AppConfig uses HikariCP, both
 *    the main thread and background threads can safely call
 *    {@code AppConfig.getConnection()} — the pool is thread-safe and
 *    hands out independent connections to each caller.
 *  • The public API is preserved (getConnection / closeConnection /
 *    isReachable / statusLabel) so that all existing callers continue to
 *    compile and work without modification.
 *  • {@code closeConnection()} is now a no-op stub; callers using
 *    try-with-resources return connections to the pool automatically.
 *    Callers that obtained a connection without try-with-resources should
 *    close it explicitly (conn.close()) to return it to the pool.
 */
public final class DBConnection {

    private DBConnection() {}

    /**
     * Borrows a connection from the shared HikariCP pool.
     * Always close the returned connection (use try-with-resources) to
     * return it to the pool.
     */
    public static Connection getConnection() throws SQLException {
        return AppConfig.getConnection();
    }

    /**
     * No-op — kept for API compatibility.
     * With HikariCP each connection is returned to the pool when it is
     * closed; there is no per-thread state to clean up here.
     */
    public static void closeConnection() {
        // No-op: pool manages connection lifecycle
    }

    /**
     * Quick connectivity test — returns true when the DB is reachable.
     * Safe to call from any thread.
     */
    public static boolean isReachable() {
        return AppConfig.isConnected();
    }

    /**
     * Returns a brief status string suitable for UI display.
     * e.g. "🟢 Database connected" or "🔴 DB offline: ..."
     */
    public static String statusLabel() {
        try (Connection test = AppConfig.getConnection()) {
            return "🟢 Database connected";
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.length() > 80) msg = msg.substring(0, 80) + "…";
            return "🔴 DB offline: " + msg;
        }
    }
}