package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.ai.AIDescriptionService;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/descriptions")
@RequiredArgsConstructor
@Tag(name = "AI - Descriptions", description = "GPT-4 powered product description generation")
public class AIDescriptionController {

    private final AIDescriptionService aiDescriptionService;
    private final ProductRepository productRepository;

    @PostMapping("/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate AI description for a product (Admin only)")
    public ResponseEntity<AIDTOs.AIDescriptionResponse> generateDescription(
        @PathVariable Long productId
    ) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        String aiDescription = aiDescriptionService.generateForProduct(productId);

        return ResponseEntity.ok(new AIDTOs.AIDescriptionResponse(
            productId,
            product.getName(),
            aiDescription,
            false
        ));
    }

    @PutMapping("/product/{productId}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force regenerate AI description for a product (Admin only)")
    public ResponseEntity<AIDTOs.AIDescriptionResponse> regenerateDescription(
        @PathVariable Long productId
    ) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        String aiDescription = aiDescriptionService.regenerateForProduct(productId);

        return ResponseEntity.ok(new AIDTOs.AIDescriptionResponse(
            productId,
            product.getName(),
            aiDescription,
            true
        ));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Async batch generate AI descriptions for all products missing one (Admin only)")
    public ResponseEntity<AIDTOs.AIBatchResponse> batchGenerate() {
        long missingCount = productRepository.findAll()
            .stream()
            .filter(p -> p.isActive() && (p.getAiDescription() == null || p.getAiDescription().isBlank()))
            .count();

        aiDescriptionService.generateForAllMissing(); // fires async

        return ResponseEntity.accepted().body(new AIDTOs.AIBatchResponse(
            "Batch generation started in background. Check product records for updates.",
            (int) missingCount
        ));
    }
}
