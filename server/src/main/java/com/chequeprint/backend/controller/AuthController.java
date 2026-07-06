package com.chequeprint.backend.controller;

import com.chequeprint.backend.dto.LoginRequest;
import com.chequeprint.backend.dto.LoginResponse;
import com.chequeprint.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("timestamp", LocalDateTime.now().toString());
            error.put("status", HttpStatus.UNAUTHORIZED.value());
            error.put("error", "Unauthorized");
            error.put("message", ex.getMessage());
            error.put("path", "/api/auth/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (IllegalStateException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("timestamp", LocalDateTime.now().toString());
            error.put("status", HttpStatus.FORBIDDEN.value());
            error.put("error", "Forbidden");
            error.put("message", ex.getMessage());
            error.put("path", "/api/auth/login");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }
}
