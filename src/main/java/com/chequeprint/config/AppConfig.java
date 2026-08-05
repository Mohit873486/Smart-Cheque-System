package com.chequeprint.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * AppConfig — provides the application-wide JDBC connection pool (HikariCP).
 *
 * Design notes:
 *  • Replaced the old single-connection DriverManager singleton with a
 *    HikariCP DataSource so that every DAO/thread can safely borrow and
 *    return connections without sharing state.
 *  • The public API (getConnection / closeConnection / isConnected) is
 *    unchanged — all existing callers continue to work without modification.
 *  • DBConnection.java now delegates to this same pool for its connections,
 *    eliminating the second set of raw DriverManager calls.
 *  • Pool is lazily initialised on first use and shut down cleanly on
 *    Application.stop() via closeConnection().
 */
public final class AppConfig {

    // ── JDBC URL ─────────────────────────────────────────────────────────
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/chequeprint_db"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Kolkata"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8"
            // Prepared-statement cache — speeds up repeated queries
            + "&cachePrepStmts=true"
            + "&prepStmtCacheSize=250"
            + "&prepStmtCacheSqlLimit=2048"
            + "&useServerPrepStmts=true"
            + "&rewriteBatchedStatements=true";

    private static final String DB_USER = "root";
    private static final String DB_PASS = "root123";

    // ── HikariCP DataSource (created once, shared by all threads) ────────
    private static volatile HikariDataSource dataSource;

    // Prevent instantiation
    private AppConfig() {}

    /**
     * Returns a pooled JDBC connection.
     * The caller is responsible for closing it (try-with-resources is recommended)
     * so that it is returned to the pool automatically.
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Shuts the pool down gracefully.
     * Call once on application shutdown (e.g. Application.stop()).
     */
    public static synchronized void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }

    /** Returns true when the database pool is up and a test connection succeeds. */
    public static boolean isConnected() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Optional helper: path to tessdata for OCR.
     * Returns system property {@code tessdata.path} or env {@code TESSDATA_PREFIX}
     * when set, otherwise null.
     */
    public static String getTessDataPath() {
        String p = System.getProperty("tessdata.path");
        if (p != null && !p.isBlank()) return p;
        String e = System.getenv("TESSDATA_PREFIX");
        if (e != null && !e.isBlank()) return e;
        return null;
    }

    // ── Internal: lazy-init pool ─────────────────────────────────────────
    private static HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            synchronized (AppConfig.class) {
                if (dataSource == null || dataSource.isClosed()) {
                    dataSource = buildPool();
                }
            }
        }
        return dataSource;
    }

    private static HikariDataSource buildPool() {
        HikariConfig cfg = new HikariConfig();

        cfg.setJdbcUrl(DB_URL);
        cfg.setUsername(DB_USER);
        cfg.setPassword(DB_PASS);
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool sizing — appropriate for a single-user desktop app
        cfg.setPoolName("ChequePrintPool");
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);

        // Timeouts (ms)
        cfg.setConnectionTimeout(5_000);   // max wait to borrow a connection
        cfg.setValidationTimeout(3_000);   // max time for isValid() check
        cfg.setIdleTimeout(300_000);       // close idle connections after 5 min
        cfg.setMaxLifetime(1_800_000);     // recycle connections after 30 min
        cfg.setKeepaliveTime(60_000);      // ping idle connections every 1 min

        // Connection health check
        cfg.setConnectionTestQuery("SELECT 1");

        // Leak detection — logs a warning if a connection is held > 15 s
        cfg.setLeakDetectionThreshold(15_000);

        return new HikariDataSource(cfg);
    }
}