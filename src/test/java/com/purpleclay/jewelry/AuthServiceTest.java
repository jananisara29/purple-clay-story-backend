package com.purpleclay.jewelry;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.model.dto.AuthDTOs;
import com.purpleclay.jewelry.model.entity.User;
import com.purpleclay.jewelry.model.enums.Role;
import com.purpleclay.jewelry.repository.UserRepository;
import com.purpleclay.jewelry.security.jwt.JwtUtil;
import com.purpleclay.jewelry.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private AuthService authService;

    private AuthDTOs.RegisterRequest registerRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDTOs.RegisterRequest(
            "Janani", "test@test.com", "password123", "9999999999"
        );

        mockUser = User.builder()
            .id(1L)
            .name("Janani")
            .email("test@test.com")
            .password("encoded-password")
            .role(Role.CUSTOMER)
            .active(true)
            .build();
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any())).thenReturn(mockUser);

        org.springframework.security.core.userdetails.UserDetails mockDetails =
            mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(mockDetails.getUsername()).thenReturn("test@test.com");
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mockDetails);
        when(jwtUtil.generateToken(any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthDTOs.AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals(Role.CUSTOMER, response.role());
    }

    @Test
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getProfile_success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        AuthDTOs.UserProfileResponse profile = authService.getProfile("test@test.com");

        assertEquals("Janani", profile.name());
        assertEquals(Role.CUSTOMER, profile.role());
    }
}
