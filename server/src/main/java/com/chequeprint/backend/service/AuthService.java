package com.chequeprint.backend.service;

import com.chequeprint.backend.dto.LoginRequest;
import com.chequeprint.backend.dto.LoginResponse;
import com.chequeprint.backend.entity.User;
import com.chequeprint.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final com.chequeprint.backend.security.JwtUtils jwtUtils;

    @Autowired
    public AuthService(UserRepository userRepository, com.chequeprint.backend.security.JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes();
            throw new AccountLockedException("Account is locked. Try again after " + minutesRemaining + " minutes.");
        }

        // Verify password
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            // Wrong password - increment failed attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                throw new AccountLockedException("Account locked due to too many failed attempts. Try again after " + LOCK_DURATION_MINUTES + " minutes.");
            }

            userRepository.save(user);
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            throw new InvalidCredentialsException("Invalid username or password.", remaining);
        }

        // Check account status
        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("User account status is not Active: " + user.getStatus());
        }

        // Success - reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Generate JWT
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        long expiresIn = 86400;

        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponse(token, expiresIn, userDto);
    }

    // Custom exceptions
    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException(String message) { super(message); }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        private final int remainingAttempts;
        public InvalidCredentialsException(String message, int remainingAttempts) {
            super(message);
            this.remainingAttempts = remainingAttempts;
        }
        public int getRemainingAttempts() { return remainingAttempts; }
    }

    @Transactional
    public void unlockUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }
}