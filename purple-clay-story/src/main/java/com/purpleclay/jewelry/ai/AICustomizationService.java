package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AICustomizationService {

    private final OpenAIClient openAIClient;
    private final ProductRepository productRepository;
    private final ImageOverlayService imageOverlayService;
    private final CustomizationValidator customizationValidator;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // Cache key: customization:productId:color:shape:hookType
    private static final String CACHE_PREFIX = "customization:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private static final String IMAGE_SYSTEM_PROMPT = """
        You are generating a DALL-E prompt for a handmade cold porcelain clay jewelry product.
        The prompt must:
        - Describe the jewelry piece with specified customization options
        - Set a clean, bright white or soft cream background (product photography style)
        - Emphasize the clay texture: smooth matte finish with handcrafted details
        - Be 1-2 sentences. No markdown. No quotes. Just the prompt text.
        """;

    public AIDTOs.AICustomizationResponse generatePreview(AIDTOs.AICustomizationRequest request) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        // Validate selections against product's allowed options
        customizationValidator.validate(product, request);

        // Check Redis cache first — DALL-E is expensive
        String cacheKey = buildCacheKey(request);
        AIDTOs.AICustomizationResponse cached = getCachedResponse(cacheKey);
        if (cached != null) {
            log.info("Returning cached customization preview for key: {}", cacheKey);
            return cached;
        }

        // Step 1: GPT-4 builds the DALL-E prompt
        String dallePrompt = buildDallePrompt(product, request);
        log.info("Generating DALL-E image for product: {} with options: color={}, shape={}, hook={}",
            product.getName(), request.color(), request.shape(), request.hookType());

        // Step 2: DALL-E 3 generates the image
        String generatedImageUrl = openAIClient.generateImage(dallePrompt);
        log.info("DALL-E image generated: {}", generatedImageUrl);

        // Step 3: Overlay on base product image (if base image exists)
        String overlayImageBase64 = null;
        if (imageOverlayService.canOverlay(product.getBaseImageUrl())) {
            overlayImageBase64 = imageOverlayService.overlayImages(
                product.getBaseImageUrl(), generatedImageUrl
            );
            if (overlayImageBase64 == null) {
                log.warn("Overlay failed for product {}, falling back to generated image only", product.getId());
            }
        }

        // Step 4: GPT-4 writes a short description of the customized piece
        String customDescription = generateCustomDescription(product, request);

        AIDTOs.AICustomizationResponse response = new AIDTOs.AICustomizationResponse(
            product.getId(),
            request.color(),
            request.shape(),
            request.hookType(),
            generatedImageUrl,
            overlayImageBase64,     // null if no base image or overlay failed
            customDescription
        );

        // Cache it
        cacheResponse(cacheKey, response);

        return response;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String buildDallePrompt(Product product, AIDTOs.AICustomizationRequest request) {
        String userMessage = String.format("""
            Product: %s (handmade cold porcelain clay jewelry)
            Category: %s
            Color: %s
            Shape: %s
            Hook type: %s
            %s
            Write a DALL-E image generation prompt for this customized jewelry piece.
            """,
            product.getName(),
            product.getCategory().getName(),
            request.color() != null ? request.color() : "natural white",
            request.shape() != null ? request.shape() : "original",
            request.hookType() != null ? request.hookType() : "standard",
            request.additionalNotes() != null ? "Additional notes: " + request.additionalNotes() : ""
        );

        return openAIClient.chat(IMAGE_SYSTEM_PROMPT, userMessage);
    }

    private String generateCustomDescription(Product product, AIDTOs.AICustomizationRequest request) {
        String systemPrompt = """
            You are a jewelry copywriter for Purple Clay Story, a handmade cold porcelain clay brand.
            Write 1 sentence describing this customer's customized jewelry piece.
            Mention the color, shape, and hook type naturally. Be warm and specific.
            """;

        String userMessage = String.format(
            "Product: %s | Color: %s | Shape: %s | Hook: %s",
            product.getName(),
            request.color() != null ? request.color() : "natural",
            request.shape() != null ? request.shape() : "standard",
            request.hookType() != null ? request.hookType() : "standard"
        );

        return openAIClient.chat(systemPrompt, userMessage);
    }

    private String buildCacheKey(AIDTOs.AICustomizationRequest request) {
        return CACHE_PREFIX
            + request.productId() + ":"
            + nvl(request.color()) + ":"
            + nvl(request.shape()) + ":"
            + nvl(request.hookType());
    }

    private String nvl(String value) {
        return value != null ? value.toLowerCase().replace(" ", "_") : "default";
    }

    private AIDTOs.AICustomizationResponse getCachedResponse(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof AIDTOs.AICustomizationResponse response) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key {}: {}", key, e.getMessage());
        }
        return null;
    }

    private void cacheResponse(String key, AIDTOs.AICustomizationResponse response) {
        try {
            redisTemplate.opsForValue().set(key, response, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key {}: {}", key, e.getMessage());
        }
    }
}
