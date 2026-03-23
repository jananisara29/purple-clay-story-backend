package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.model.dto.AuthDTOs;
import com.purpleclay.jewelry.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<AuthDTOs.AuthResponse> register(
        @Valid @RequestBody AuthDTOs.RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<AuthDTOs.AuthResponse> login(
        @Valid @RequestBody AuthDTOs.LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthDTOs.AuthResponse> refresh(
        @Valid @RequestBody AuthDTOs.RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current logged-in user profile")
    public ResponseEntity<AuthDTOs.UserProfileResponse> getProfile(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(authService.getProfile(userDetails.getUsername()));
    }
}
