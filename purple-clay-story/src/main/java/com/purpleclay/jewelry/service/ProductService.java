package com.purpleclay.jewelry.service;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.model.entity.Category;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.CategoryRepository;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    // ─── Read ────────────────────────────────────────────────────────────────

    @Cacheable(value = "products", key = "#id")
    public ProductDTOs.ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!product.isActive()) throw new ResourceNotFoundException("Product", id);
        return productMapper.toResponse(product);
    }

    public ProductDTOs.PagedResponse<ProductDTOs.ProductResponse> searchProducts(
        ProductDTOs.ProductSearchParams params
    ) {
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(params.sortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            params.sortBy() != null ? params.sortBy() : "createdAt"
        );

        Pageable pageable = PageRequest.of(params.page(), params.size(), sort);

        Page<Product> page = productRepository.searchProducts(
            params.categoryId(),
            params.minPrice(),
            params.maxPrice(),
            params.customizable(),
            params.search(),
            pageable
        );

        Page<ProductDTOs.ProductResponse> responsePage = page.map(productMapper::toResponse);
        return productMapper.toPagedResponse(responsePage);
    }

    public List<ProductDTOs.ProductResponse> getLatestProducts() {
        return productRepository.findTop10ByActiveTrueOrderByCreatedAtDesc()
            .stream()
            .map(productMapper::toResponse)
            .toList();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTOs.ProductResponse createProduct(ProductDTOs.ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        if (productRepository.existsByNameAndCategoryId(request.name(), request.categoryId())) {
            throw new BadRequestException("Product '" + request.name() + "' already exists in this category");
        }

        Product product = Product.builder()
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .stockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0)
            .imageUrl(request.imageUrl())
            .baseImageUrl(request.baseImageUrl())
            .customizable(request.customizable())
            .availableColors(request.availableColors())
            .availableShapes(request.availableShapes())
            .availableHookTypes(request.availableHookTypes())
            .category(category)
            .build();

        product = productRepository.save(product);
        log.info("Product created: {} in category: {}", product.getName(), category.getName());
        return productMapper.toResponse(product);
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "recommendations", allEntries = true)
    })
    public ProductDTOs.ProductResponse updateProduct(Long id, ProductDTOs.ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.stockQuantity() != null) product.setStockQuantity(request.stockQuantity());
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.baseImageUrl() != null) product.setBaseImageUrl(request.baseImageUrl());
        if (request.customizable() != null) product.setCustomizable(request.customizable());
        if (request.availableColors() != null) product.setAvailableColors(request.availableColors());
        if (request.availableShapes() != null) product.setAvailableShapes(request.availableShapes());
        if (request.availableHookTypes() != null) product.setAvailableHookTypes(request.availableHookTypes());
        if (request.active() != null) product.setActive(request.active());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            product.setCategory(category);
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false); // Soft delete
        productRepository.save(product);
        log.info("Product soft-deleted: {}", id);
    }

    // ─── Stock management ────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void decrementStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    // ─── Update AI Description (called from AI module) ───────────────────────

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void updateAiDescription(Long id, String aiDescription) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setAiDescription(aiDescription);
        productRepository.save(product);
    }
}
