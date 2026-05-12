package com.chequeprint.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * DateUtil — reusable date formatting and calculation helpers.
 *
 * All methods are static; the class is not instantiable.
 */
public final class DateUtil {

    // ── Common formatters ────────────────────────────────────────────
    /** dd/MM/yyyy  — common Indian display format */
    public static final DateTimeFormatter DD_MM_YYYY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** dd-MM-yyyy */
    public static final DateTimeFormatter DD_MM_YYYY_DASH =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** dd MMM yyyy  e.g. 15 Jan 2025 */
    public static final DateTimeFormatter DD_MMM_YYYY =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    /** ISO  yyyy-MM-dd  (DB / API format) */
    public static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private DateUtil() {}

    // ── Format helpers ───────────────────────────────────────────────
    /** Returns today in "dd/MM/yyyy" format. */
    public static String todayFormatted() {
        return LocalDate.now().format(DD_MM_YYYY);
    }

    /** Formats a LocalDate using the standard display pattern dd/MM/yyyy. */
    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DD_MM_YYYY);
    }

    /** Formats a LocalDate with a custom formatter. */
    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date == null ? "" : date.format(formatter);
    }

    /** Formats a LocalDate for database storage (yyyy-MM-dd). */
    public static String formatForDb(LocalDate date) {
        return date == null ? "" : date.format(ISO);
    }

    // ── Parse helpers ────────────────────────────────────────────────
    /**
     * Tries to parse a string as LocalDate.
     * Accepts dd/MM/yyyy, dd-MM-yyyy, and yyyy-MM-dd.
     * Returns null on failure instead of throwing.
     */
    public static LocalDate parseOrNull(String text) {
        if (text == null || text.isBlank()) return null;
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{DD_MM_YYYY, DD_MM_YYYY_DASH, ISO}) {
            try {
                return LocalDate.parse(text.trim(), fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // ── Calculation helpers ──────────────────────────────────────────
    /** Returns true when the given date is strictly in the past. */
    public static boolean isOverdue(LocalDate dueDate) {
        return dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    /** Returns the number of days between today and a future due date (negative = overdue). */
    public static long daysUntilDue(LocalDate dueDate) {
        return dueDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /** Returns the first day of the current calendar month. */
    public static LocalDate startOfCurrentMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /** Returns the last day of the current calendar month. */
    public static LocalDate endOfCurrentMonth() {
        LocalDate today = LocalDate.now();
        return today.withDayOfMonth(today.lengthOfMonth());
    }

    /** Returns a human-readable "X days overdue" or "Due in X days" label. */
    public static String dueSummary(LocalDate dueDate) {
        if (dueDate == null) return "No due date";
        long days = daysUntilDue(dueDate);
        if (days < 0)  return Math.abs(days) + " day(s) overdue";
        if (days == 0) return "Due today";
        return "Due in " + days + " day(s)";
    }
}