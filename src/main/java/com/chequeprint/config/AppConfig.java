package com.chequeprint.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * AppConfig — provides the application-wide JDBC connection.
 *
 * Fixes applied vs original:
 *  • Added URL parameters: useSSL=false, allowPublicKeyRetrieval=true,
 *    serverTimezone, autoReconnect — prevents common MySQL 8 connection errors.
 *  • synchronized getConnection() prevents race conditions on startup.
 *  • Added closeConnection() for clean shutdown.
 *  • Kept singleton pattern; DAOs running on the JavaFX Application Thread
 *    all share this one connection (safe for single-user desktop app).
 *    Background threads should use DBConnection.getConnection() instead.
 */
public final class AppConfig {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/chequeprint_db"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Kolkata"
            + "&autoReconnect=true"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8";

    private static final String DB_USER = "root";
    private static final String DB_PASS = "root123";

    private static Connection connection = null;

    // Prevent instantiation
    private AppConfig() {}

    /**
     * Returns the shared JDBC connection, creating or reopening it as needed.
     * Thread-safe via synchronized.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        return connection;
    }

    /**
     * Closes the shared connection gracefully.
     * Call on application shutdown (e.g. Application.stop()).
     */
    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
            connection = null;
        }
    }

    /** Returns true when the database can be reached. */
    public static boolean isConnected() {
        try {
            Connection c = getConnection();
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}