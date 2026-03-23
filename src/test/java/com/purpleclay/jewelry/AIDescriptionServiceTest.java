package com.purpleclay.jewelry;

import com.purpleclay.jewelry.ai.AIDescriptionService;
import com.purpleclay.jewelry.ai.OpenAIClient;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.entity.Category;
import com.purpleclay.jewelry.model.entity.Product;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIDescriptionServiceTest {

    @Mock private OpenAIClient openAIClient;
    @Mock private ProductRepository productRepository;
    @Mock private ProductService productService;
    @Mock private ProductMapper productMapper;

    @InjectMocks private AIDescriptionService aiDescriptionService;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).name("Earrings").build();
        mockProduct = Product.builder()
            .id(1L)
            .name("Gold Leaf Studs")
            .description("Handcrafted leaf-shaped earrings")
            .price(new BigDecimal("599.00"))
            .active(true)
            .customizable(true)
            .category(category)
            .build();
    }

    @Test
    void generateForProduct_success_savesDescription() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productMapper.parseJsonList(any())).thenReturn(java.util.List.of());
        when(openAIClient.chat(anyString(), anyString()))
            .thenReturn("  Delicate gold leaf studs handcrafted from cold porcelain clay.  ");

        String result = aiDescriptionService.generateForProduct(1L);

        assertEquals("Delicate gold leaf studs handcrafted from cold porcelain clay.", result);
        verify(productService).updateAiDescription(eq(1L), eq("Delicate gold leaf studs handcrafted from cold porcelain clay."));
    }

    @Test
    void generateForProduct_productNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> aiDescriptionService.generateForProduct(99L));

        verify(openAIClient, never()).chat(any(), any());
    }

    @Test
    void regenerateForProduct_callsOpenAIAndSaves() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productMapper.parseJsonList(any())).thenReturn(java.util.List.of());
        when(openAIClient.chat(anyString(), anyString()))
            .thenReturn("Each stud is shaped by hand from cold porcelain clay.");

        String result = aiDescriptionService.regenerateForProduct(1L);

        assertNotNull(result);
        verify(openAIClient, times(1)).chat(anyString(), anyString());
        verify(productService).updateAiDescription(eq(1L), anyString());
    }

    @Test
    void generateForProduct_openAIFails_throwsRuntimeException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productMapper.parseJsonList(any())).thenReturn(java.util.List.of());
        when(openAIClient.chat(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI service unavailable"));

        assertThrows(RuntimeException.class,
            () -> aiDescriptionService.generateForProduct(1L));

        verify(productService, never()).updateAiDescription(any(), any());
    }
}
