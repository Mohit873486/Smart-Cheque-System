package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;
import com.chequeprint.util.PasswordUtil;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ForgotPasswordService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 10;

    private final UserDAO dao = new UserDAO();

    public String startReset(String usernameOrEmail) throws SQLException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new IllegalArgumentException("Enter your username or email first.");
        }

        User user = dao.findByUsernameOrEmail(usernameOrEmail.trim());
        if (user == null) {
            return null;
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        dao.createPasswordResetOtp(user.getId(), PasswordUtil.hash(otp), LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        return otp;
    }

    public void resetPassword(String usernameOrEmail, String otp, String newPassword) throws SQLException {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must be a 6-digit code.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        User user = dao.findByUsernameOrEmail(usernameOrEmail.trim());
        if (user == null) {
            throw new IllegalArgumentException("Invalid OTP or user.");
        }

        String otpHash = dao.findActiveOtpHash(user.getId());
        if (!PasswordUtil.matches(otp, otpHash)) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        dao.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
        dao.markOtpsUsed(user.getId());
    }
}
