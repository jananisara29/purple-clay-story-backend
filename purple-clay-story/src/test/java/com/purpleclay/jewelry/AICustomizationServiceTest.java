package com.purpleclay.jewelry;

import com.purpleclay.jewelry.ai.*;
import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.entity.Category;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.util.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AICustomizationServiceTest {

    @Mock private OpenAIClient openAIClient;
    @Mock private ProductRepository productRepository;
    @Mock private ImageOverlayService imageOverlayService;
    @Mock private CustomizationValidator customizationValidator;
    @Mock private ProductMapper productMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private AICustomizationService aiCustomizationService;

    private Product mockProduct;
    private AIDTOs.AICustomizationRequest validRequest;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).name("Earrings").build();
        mockProduct = Product.builder()
            .id(1L)
            .name("Daisy Hoops")
            .price(new BigDecimal("749.00"))
            .active(true)
            .customizable(true)
            .availableColors("[\"gold\",\"silver\",\"rose-gold\"]")
            .availableShapes("[\"round\",\"teardrop\"]")
            .availableHookTypes("[\"hoop\",\"stud\"]")
            .baseImageUrl("https://example.com/daisy-base.jpg")
            .category(category)
            .build();

        validRequest = new AIDTOs.AICustomizationRequest(
            1L, "gold", "round", "hoop", null
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void generatePreview_cacheHit_returnsCachedResponse() {
        AIDTOs.AICustomizationResponse cachedResponse = new AIDTOs.AICustomizationResponse(
            1L, "gold", "round", "hoop",
            "https://dalle.url/img.png", null, "Cached description"
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        doNothing().when(customizationValidator).validate(any(), any());
        when(valueOperations.get(anyString())).thenReturn(cachedResponse);

        AIDTOs.AICustomizationResponse result = aiCustomizationService.generatePreview(validRequest);

        assertEquals("gold", result.color());
        assertEquals("Cached description", result.description());
        // OpenAI should NOT be called on cache hit
        verify(openAIClient, never()).generateImage(any());
    }

    @Test
    void generatePreview_cacheMiss_callsDalleAndOverlay() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        doNothing().when(customizationValidator).validate(any(), any());
        when(valueOperations.get(anyString())).thenReturn(null);
        when(openAIClient.chat(anyString(), anyString()))
            .thenReturn("A gold round daisy hoop earring prompt")
            .thenReturn("Your customized gold daisy hoop earring.");
        when(openAIClient.generateImage(anyString())).thenReturn("https://dalle.url/new.png");
        when(imageOverlayService.canOverlay(anyString())).thenReturn(true);
        when(imageOverlayService.overlayImages(anyString(), anyString())).thenReturn("data:image/png;base64,abc");
        doNothing().when(valueOperations).set(anyString(), any(), any());

        AIDTOs.AICustomizationResponse result = aiCustomizationService.generatePreview(validRequest);

        assertNotNull(result);
        assertEquals("https://dalle.url/new.png", result.generatedImageUrl());
        assertEquals("data:image/png;base64,abc", result.overlayImageUrl());
        verify(openAIClient, times(2)).chat(anyString(), anyString()); // prompt + description
        verify(openAIClient, times(1)).generateImage(anyString());
    }

    @Test
    void generatePreview_productNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        AIDTOs.AICustomizationRequest badRequest = new AIDTOs.AICustomizationRequest(
            99L, "gold", "round", "hoop", null
        );

        assertThrows(ResourceNotFoundException.class,
            () -> aiCustomizationService.generatePreview(badRequest));
        verify(openAIClient, never()).generateImage(any());
    }

    @Test
    void generatePreview_overlayFails_returnsResponseWithNullOverlay() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        doNothing().when(customizationValidator).validate(any(), any());
        when(valueOperations.get(anyString())).thenReturn(null);
        when(openAIClient.chat(anyString(), anyString()))
            .thenReturn("A dalle prompt")
            .thenReturn("A custom description.");
        when(openAIClient.generateImage(anyString())).thenReturn("https://dalle.url/new.png");
        when(imageOverlayService.canOverlay(anyString())).thenReturn(true);
        when(imageOverlayService.overlayImages(anyString(), anyString())).thenReturn(null); // overlay fails
        doNothing().when(valueOperations).set(anyString(), any(), any());

        AIDTOs.AICustomizationResponse result = aiCustomizationService.generatePreview(validRequest);

        assertNotNull(result.generatedImageUrl());
        assertNull(result.overlayImageUrl()); // graceful fallback
    }
}
