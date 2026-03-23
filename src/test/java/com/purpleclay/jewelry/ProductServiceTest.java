package com.purpleclay.jewelry;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.model.entity.Category;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.CategoryRepository;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.service.ProductService;
import com.purpleclay.jewelry.util.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks private ProductService productService;

    private Category mockCategory;
    private Product mockProduct;
    private ProductDTOs.ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        mockCategory = Category.builder()
            .id(1L).name("Earrings").build();

        mockProduct = Product.builder()
            .id(1L)
            .name("Rose Gold Studs")
            .price(new BigDecimal("499.00"))
            .stockQuantity(10)
            .active(true)
            .category(mockCategory)
            .build();

        productRequest = new ProductDTOs.ProductRequest(
            "Rose Gold Studs", "Elegant studs",
            new BigDecimal("499.00"), 1L, 10,
            null, null, false, null, null, null
        );
    }

    @Test
    void createProduct_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.existsByNameAndCategoryId(any(), any())).thenReturn(false);
        when(productRepository.save(any())).thenReturn(mockProduct);

        ProductDTOs.ProductResponse mockResponse = mock(ProductDTOs.ProductResponse.class);
        when(productMapper.toResponse(any())).thenReturn(mockResponse);

        ProductDTOs.ProductResponse result = productService.createProduct(productRequest);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_categoryNotFound_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(productRequest));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_duplicateName_throwsBadRequest() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.existsByNameAndCategoryId(any(), any())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> productService.createProduct(productRequest));
    }

    @Test
    void getProductById_inactive_throwsNotFound() {
        mockProduct.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(1L));
    }

    @Test
    void deleteProduct_softDelete_setsActiveFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any())).thenReturn(mockProduct);

        productService.deleteProduct(1L);

        assertFalse(mockProduct.isActive());
        verify(productRepository).save(mockProduct);
    }

    @Test
    void decrementStock_insufficientStock_throwsBadRequest() {
        mockProduct.setStockQuantity(2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        assertThrows(BadRequestException.class, () -> productService.decrementStock(1L, 5));
    }
}
