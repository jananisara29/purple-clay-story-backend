package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product CRUD, search, and filtering")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search and filter products with pagination")
    public ResponseEntity<ProductDTOs.PagedResponse<ProductDTOs.ProductResponse>> searchProducts(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean customizable,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        ProductDTOs.ProductSearchParams params = new ProductDTOs.ProductSearchParams(
            categoryId, minPrice, maxPrice, customizable, search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(productService.searchProducts(params));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get 10 latest products")
    public ResponseEntity<List<ProductDTOs.ProductResponse>> getLatestProducts() {
        return ResponseEntity.ok(productService.getLatestProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDTOs.ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product (Admin only)")
    public ResponseEntity<ProductDTOs.ProductResponse> createProduct(
        @Valid @RequestBody ProductDTOs.ProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product fields (Admin only)")
    public ResponseEntity<ProductDTOs.ProductResponse> updateProduct(
        @PathVariable Long id,
        @RequestBody ProductDTOs.ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete product (Admin only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
