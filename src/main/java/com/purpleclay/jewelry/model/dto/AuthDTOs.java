package com.purpleclay.jewelry.model.dto;

import com.purpleclay.jewelry.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTOs {

    // ─── Requests ──────────────────────────────────────────────────────────

    public record RegisterRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String phone
    ) {}

    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
    ) {}

    // ─── Responses ─────────────────────────────────────────────────────────

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role
    ) {
        public static AuthResponse of(String accessToken, String refreshToken,
                                      Long userId, String name, String email, Role role) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", userId, name, email, role);
        }
    }

    public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        boolean active
    ) {}
}
