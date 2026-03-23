package com.purpleclay.jewelry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purpleclay.jewelry.ai.AIRecommendationService;
import com.purpleclay.jewelry.ai.OpenAIClient;
import com.purpleclay.jewelry.ai.UserSignalAggregator;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIRecommendationServiceTest {

    @Mock private OpenAIClient openAIClient;
    @Mock private UserSignalAggregator signalAggregator;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AIRecommendationService recommendationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Product mockProduct;

    @BeforeEach
    void setUp() throws Exception {
        // Inject real ObjectMapper via reflection (InjectMocks uses it)
        var field = AIRecommendationService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(recommendationService, objectMapper);

        Category category = Category.builder().id(1L).name("Earrings").build();
        mockProduct = Product.builder()
            .id(1L).name("Gold Leaf Studs")
            .price(new BigDecimal("599.00"))
            .active(true).customizable(true)
            .category(category).build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getPersonalized_noSignalData_returnsFallback() {
        UserSignalAggregator.UserSignalProfile emptyProfile = new UserSignalAggregator.UserSignalProfile(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(signalAggregator.buildProfile(1L)).thenReturn(emptyProfile);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(productRepository.findTop10ByActiveTrueOrderByCreatedAtDesc())
            .thenReturn(List.of(mockProduct));

        var mockResponse = mock(com.purpleclay.jewelry.model.dto.ProductDTOs.ProductResponse.class);
        when(productMapper.toResponse(any())).thenReturn(mockResponse);

        AIDTOs.AIRecommendationResponse response = recommendationService.getPersonalizedRecommendations(1L);

        assertNotNull(response);
        assertEquals("Our latest handcrafted pieces, just for you.", response.reasoning());
        verify(openAIClient, never()).chat(any(), any()); // GPT not called for fallback
    }

    @Test
    void getPersonalized_withSignalData_callsGPT() throws Exception {
        UserSignalAggregator.UserSignalProfile profile = new UserSignalAggregator.UserSignalProfile(
            List.of("Gold Leaf Studs (Earrings)"),
            List.of("Earrings"),
            List.of(2L),
            List.of("Earrings"),
            List.of(2L),
            List.of("Gold Leaf Studs in Earrings")
        );
        when(signalAggregator.buildProfile(1L)).thenReturn(profile);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(productRepository.findByActiveTrue(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(mockProduct)));
        when(openAIClient.chat(anyString(), anyString())).thenReturn("[1]");

        var mockResponse = mock(com.purpleclay.jewelry.model.dto.ProductDTOs.ProductResponse.class);
        when(productMapper.toResponse(any())).thenReturn(mockResponse);
        doNothing().when(valueOperations).set(anyString(), any(), any());

        AIDTOs.AIRecommendationResponse response = recommendationService.getPersonalizedRecommendations(1L);

        assertNotNull(response);
        verify(openAIClient, times(1)).chat(anyString(), anyString());
    }

    @Test
    void getPersonalized_cacheHit_skipsGPT() {
        AIDTOs.AIRecommendationResponse cached = new AIDTOs.AIRecommendationResponse(
            List.of(), "Cached reasoning"
        );
        when(valueOperations.get(anyString())).thenReturn(cached);
        when(signalAggregator.buildProfile(1L)).thenReturn(
            new UserSignalAggregator.UserSignalProfile(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            )
        );

        AIDTOs.AIRecommendationResponse response = recommendationService.getPersonalizedRecommendations(1L);

        assertEquals("Cached reasoning", response.reasoning());
        verify(openAIClient, never()).chat(any(), any());
    }

    @Test
    void getSimilar_invalidProductId_returnsFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(productRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        when(productRepository.findTop10ByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of());

        AIDTOs.AIRecommendationResponse response = recommendationService.getSimilarProducts(99L);
        assertNotNull(response);
        assertEquals("Our latest handcrafted pieces, just for you.", response.reasoning());
    }
}
