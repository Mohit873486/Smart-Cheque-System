package com.chequeprint.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private static final int COST = 12;

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (!isBcryptHash(storedHash)) {
            return rawPassword.equals(storedHash);
        }
        return BCrypt.checkpw(rawPassword, storedHash);
    }

    public static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
