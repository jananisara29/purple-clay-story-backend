package com.purpleclay.jewelry.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductDTOs {

    // ─── Category Requests ──────────────────────────────────────────────────

    public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        String description,
        String imageUrl
    ) {}

    // ─── Category Responses ─────────────────────────────────────────────────

    public record CategoryResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        int productCount,
        LocalDateTime createdAt
    ) {}

    // ─── Product Requests ───────────────────────────────────────────────────

    public record ProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Min(value = 0, message = "Stock cannot be negative")
        Integer stockQuantity,

        String imageUrl,
        String baseImageUrl,

        boolean customizable,

        // JSON arrays as comma-separated or JSON string
        String availableColors,
        String availableShapes,
        String availableHookTypes
    ) {}

    public record ProductUpdateRequest(
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        Integer stockQuantity,
        String imageUrl,
        String baseImageUrl,
        Boolean customizable,
        String availableColors,
        String availableShapes,
        String availableHookTypes,
        Boolean active
    ) {}

    // ─── Product Responses ──────────────────────────────────────────────────

    public record ProductResponse(
        Long id,
        String name,
        String description,
        String aiDescription,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        boolean customizable,
        List<String> availableColors,
        List<String> availableShapes,
        List<String> availableHookTypes,
        boolean active,
        CategorySummary category,
        LocalDateTime createdAt
    ) {}

    public record CategorySummary(Long id, String name) {}

    // ─── Search / Filter params ─────────────────────────────────────────────

    public record ProductSearchParams(
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean customizable,
        String search,
        int page,
        int size,
        String sortBy,       // price, createdAt, name
        String sortDir       // asc, desc
    ) {}

    // ─── Paginated Response ─────────────────────────────────────────────────

    public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
    ) {}
}
