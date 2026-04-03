package com.naas.backend.auth.controller;

import com.naas.backend.auth.dto.AuthResponse;
import com.naas.backend.auth.dto.LoginRequest;
import com.naas.backend.auth.dto.SignupRequest;
import com.naas.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("pending review") || msg.contains("rejected") || msg.contains("Invalid email or password") || msg.contains("Account disabled"))) {
                return ResponseEntity.status(403).body(java.util.Map.of("message", msg));
            }
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Internal server error"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.registerCustomer(request));
    }
}
