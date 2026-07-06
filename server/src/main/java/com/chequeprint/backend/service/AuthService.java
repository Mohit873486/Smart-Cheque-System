package com.chequeprint.backend.service;

import com.chequeprint.backend.dto.LoginRequest;
import com.chequeprint.backend.dto.LoginResponse;
import com.chequeprint.backend.entity.User;
import com.chequeprint.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final com.chequeprint.backend.security.JwtUtils jwtUtils;

    @Autowired
    public AuthService(UserRepository userRepository, com.chequeprint.backend.security.JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        User user = userOpt.get();
        
        // Verify password hash using BCrypt
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("User account status is not Active: " + user.getStatus());
        }

        // Generate a real signed JWT token using JwtUtils
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        long expiresIn = 86400; // 24 hours in seconds

        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponse(token, expiresIn, userDto);
    }
}
