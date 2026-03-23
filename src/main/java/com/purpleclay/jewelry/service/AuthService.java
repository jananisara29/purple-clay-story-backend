package com.purpleclay.jewelry.service;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.model.dto.AuthDTOs;
import com.purpleclay.jewelry.model.entity.User;
import com.purpleclay.jewelry.model.enums.Role;
import com.purpleclay.jewelry.repository.UserRepository;
import com.purpleclay.jewelry.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthDTOs.AuthResponse register(AuthDTOs.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }

        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .phone(request.phone())
            .role(Role.CUSTOMER)
            .build();

        user = userRepository.save(user);
        log.info("New customer registered: {}", user.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthDTOs.AuthResponse.of(
            accessToken, refreshToken,
            user.getId(), user.getName(), user.getEmail(), user.getRole()
        );
    }

    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadRequestException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        log.info("User logged in: {}", user.getEmail());

        return AuthDTOs.AuthResponse.of(
            accessToken, refreshToken,
            user.getId(), user.getName(), user.getEmail(), user.getRole()
        );
    }

    public AuthDTOs.AuthResponse refreshToken(AuthDTOs.RefreshTokenRequest request) {
        String email = jwtUtil.extractUsername(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtUtil.isTokenValid(request.refreshToken(), userDetails)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found"));

        String newAccessToken = jwtUtil.generateToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthDTOs.AuthResponse.of(
            newAccessToken, newRefreshToken,
            user.getId(), user.getName(), user.getEmail(), user.getRole()
        );
    }

    public AuthDTOs.UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found"));

        return new AuthDTOs.UserProfileResponse(
            user.getId(), user.getName(), user.getEmail(),
            user.getPhone(), user.getRole(), user.isActive()
        );
    }
}
