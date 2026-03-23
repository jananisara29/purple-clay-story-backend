package com.purpleclay.jewelry.model.dto;

public class AIDTOs {

    public record AIDescriptionResponse(
        Long productId,
        String productName,
        String aiDescription,
        boolean regenerated
    ) {}

    public record AIBatchResponse(
        String message,
        int totalProductsQueued
    ) {}

    public record AIChatMessage(
        String role,   // "user" or "assistant"
        String content
    ) {}

    public record AIChatRequest(
        String message,
        java.util.List<AIChatMessage> history  // optional conversation history
    ) {}

    public record AIChatResponse(
        String reply,
        java.util.List<AIChatMessage> updatedHistory
    ) {}

    public record AICustomizationRequest(
        Long productId,
        String color,
        String shape,
        String hookType,
        String additionalNotes  // optional: "make it more minimalist"
    ) {}

    public record AICustomizationResponse(
        Long productId,
        String color,
        String shape,
        String hookType,
        String generatedImageUrl,
        String overlayImageUrl,
        String description
    ) {}

    public record AIRecommendationResponse(
        java.util.List<com.purpleclay.jewelry.model.dto.ProductDTOs.ProductResponse> products,
        String reasoning
    ) {}
}
