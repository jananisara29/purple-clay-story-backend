package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.ai.AIRecommendationService;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.repository.UserRepository;
import com.purpleclay.jewelry.service.BrowseTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI - Recommendations", description = "GPT-4 powered product recommendations")
public class AIRecommendationController {

    private final AIRecommendationService recommendationService;
    private final BrowseTrackingService browseTrackingService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get personalized recommendations for logged-in user",
               description = "Uses browse history + purchase history. Falls back to latest products for new users.")
    public ResponseEntity<AIDTOs.AIRecommendationResponse> getPersonalized(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(recommendationService.getSimilarProducts(null));
        }
        return ResponseEntity.ok(recommendationService.getPersonalizedRecommendations(userId));
    }

    @GetMapping("/similar/{productId}")
    @Operation(summary = "Get similar / complementary products for a product detail page")
    public ResponseEntity<AIDTOs.AIRecommendationResponse> getSimilar(
        @PathVariable Long productId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Async track browse event for logged-in users
        Long userId = resolveUserId(userDetails);
        if (userId != null) {
            browseTrackingService.track(userId, productId);
        }

        return ResponseEntity.ok(recommendationService.getSimilarProducts(productId));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
            .map(u -> u.getId())
            .orElse(null);
    }
}
