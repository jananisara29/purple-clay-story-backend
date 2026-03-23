package com.purpleclay.jewelry.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.model.entity.Category;
import com.purpleclay.jewelry.model.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMapper {

    private final ObjectMapper objectMapper;

    public ProductDTOs.ProductResponse toResponse(Product product) {
        return new ProductDTOs.ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getAiDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getImageUrl(),
            product.isCustomizable(),
            parseJsonList(product.getAvailableColors()),
            parseJsonList(product.getAvailableShapes()),
            parseJsonList(product.getAvailableHookTypes()),
            product.isActive(),
            new ProductDTOs.CategorySummary(
                product.getCategory().getId(),
                product.getCategory().getName()
            ),
            product.getCreatedAt()
        );
    }

    public ProductDTOs.CategoryResponse toCategoryResponse(Category category) {
        return new ProductDTOs.CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getImageUrl(),
            category.getProducts() != null ? category.getProducts().size() : 0,
            category.getCreatedAt()
        );
    }

    public <T> ProductDTOs.PagedResponse<T> toPagedResponse(Page<T> page) {
        return new ProductDTOs.PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    public List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", json);
            return Collections.emptyList();
        }
    }

    public String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to serialize list to JSON");
            return null;
        }
    }
}
