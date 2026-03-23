package com.purpleclay.jewelry.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {

    private final OpenAIClient openAIClient;
    private final UserSignalAggregator signalAggregator;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "recommendations:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final int CATALOG_SAMPLE_SIZE = 30;
    private static final int MAX_RECOMMENDATIONS = 6;

    private static final String SYSTEM_PROMPT = """
        You are a recommendation engine for Purple Clay Story, a handmade cold porcelain clay jewelry brand.
        Given a user's browse and purchase history, and a product catalog, select the best products to recommend.
        
        Rules:
        - Return ONLY a valid JSON array of product IDs (Long integers): e.g. [3, 7, 12, 5]
        - Maximum %d recommendations
        - Prefer products from categories the user browses most
        - Do NOT recommend products already purchased
        - Prioritize customizable products if user has shown interest
        - No explanation. No markdown. Just the JSON array.
        """.formatted(MAX_RECOMMENDATIONS);

    /**
     * Personalized recommendations for a logged-in user.
     */
    public AIDTOs.AIRecommendationResponse getPersonalizedRecommendations(Long userId) {
        // Check Redis cache first
        String cacheKey = CACHE_PREFIX + userId;
        AIDTOs.AIRecommendationResponse cached = getCached(cacheKey);
        if (cached != null) {
            log.debug("Returning cached recommendations for user {}", userId);
            return cached;
        }

        // Build user signal profile
        UserSignalAggregator.UserSignalProfile profile = signalAggregator.buildProfile(userId);

        // If no signal data, return latest products as fallback
        if (profile.isEmpty()) {
            log.info("No signal data for user {}, returning latest products", userId);
            return buildFallbackResponse();
        }

        // Sample available catalog (exclude already purchased/viewed)
        List<Product> catalogSample = getCatalogSample(profile.excludeProductIds());
        if (catalogSample.isEmpty()) return buildFallbackResponse();

        // Ask GPT-4 to pick IDs
        String userContext = buildUserContext(profile);
        String catalogContext = buildCatalogContext(catalogSample);
        String gptPrompt = userContext + "\n\nAVAILABLE CATALOG:\n" + catalogContext;

        List<Long> recommendedIds = askGPTForIds(gptPrompt);

        // Fetch products in recommended order
        List<ProductDTOs.ProductResponse> products = fetchOrderedProducts(recommendedIds, catalogSample);

        // Generate human-readable reasoning
        String reasoning = generateReasoning(profile, products);

        AIDTOs.AIRecommendationResponse response = new AIDTOs.AIRecommendationResponse(products, reasoning);
        cacheResponse(cacheKey, response);
        return response;
    }

    /**
     * Similar products for a product detail page (no user context needed).
     */
    public AIDTOs.AIRecommendationResponse getSimilarProducts(Long productId) {
        String cacheKey = "recommendations:similar:" + productId;
        AIDTOs.AIRecommendationResponse cached = getCached(cacheKey);
        if (cached != null) return cached;

        Product source = productRepository.findById(productId).orElse(null);
        if (source == null) return buildFallbackResponse();

        // Get related from same category
        List<Product> related = productRepository.findRelatedProducts(
            source.getCategory().getId(), productId, PageRequest.of(0, 10)
        );

        if (related.isEmpty()) return buildFallbackResponse();

        String prompt = String.format("""
            Source product: %s (Category: %s, Price: ₹%.0f)
            
            AVAILABLE PRODUCTS:
            %s
            
            Pick the most similar/complementary products from the list above.
            Return ONLY a JSON array of product IDs. No explanation. No markdown.
            """,
            source.getName(),
            source.getCategory().getName(),
            source.getPrice(),
            buildCatalogContext(related)
        );

        List<Long> ids = askGPTForIds(prompt);
        List<ProductDTOs.ProductResponse> products = fetchOrderedProducts(ids, related);
        String reasoning = "Because you're viewing " + source.getName() + ", you might also like these.";

        AIDTOs.AIRecommendationResponse response = new AIDTOs.AIRecommendationResponse(products, reasoning);
        cacheResponse(cacheKey, response);
        return response;
    }

    /**
     * Track a browse event — called from product detail view.
     */
    public void trackView(Long userId, Product product) {
        // Invalidate user recommendation cache on new browse signal
        redisTemplate.delete(CACHE_PREFIX + userId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String buildUserContext(UserSignalAggregator.UserSignalProfile profile) {
        StringBuilder sb = new StringBuilder("USER BEHAVIOR PROFILE:\n");

        if (!profile.recentlyViewed().isEmpty()) {
            sb.append("Recently viewed: ").append(String.join(", ", profile.recentlyViewed())).append("\n");
        }
        if (!profile.topBrowsedCategories().isEmpty()) {
            sb.append("Browses most: ").append(String.join(", ", profile.topBrowsedCategories())).append("\n");
        }
        if (!profile.purchaseHistory().isEmpty()) {
            sb.append("Previously purchased: ").append(String.join(", ", profile.purchaseHistory())).append("\n");
        }
        if (!profile.purchasedCategories().isEmpty()) {
            sb.append("Bought from categories: ").append(String.join(", ", profile.purchasedCategories())).append("\n");
        }

        return sb.toString();
    }

    private String buildCatalogContext(List<Product> products) {
        return products.stream()
            .map(p -> String.format("ID:%d | %s | ₹%.0f | %s | Customizable:%s",
                p.getId(), p.getName(), p.getPrice(),
                p.getCategory().getName(), p.isCustomizable() ? "Yes" : "No"))
            .collect(Collectors.joining("\n"));
    }

    private List<Long> askGPTForIds(String userPrompt) {
        try {
            String raw = openAIClient.chat(SYSTEM_PROMPT, userPrompt);
            // Strip any markdown fences just in case
            raw = raw.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Failed to parse GPT recommendation IDs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Product> getCatalogSample(Set<Long> excludeIds) {
        return productRepository.findByActiveTrue(PageRequest.of(0, CATALOG_SAMPLE_SIZE))
            .getContent()
            .stream()
            .filter(p -> !excludeIds.contains(p.getId()))
            .collect(Collectors.toList());
    }

    private List<ProductDTOs.ProductResponse> fetchOrderedProducts(
        List<Long> orderedIds, List<Product> catalog
    ) {
        Map<Long, Product> catalogMap = catalog.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        return orderedIds.stream()
            .filter(catalogMap::containsKey)
            .map(catalogMap::get)
            .map(productMapper::toResponse)
            .limit(MAX_RECOMMENDATIONS)
            .collect(Collectors.toList());
    }

    private String generateReasoning(
        UserSignalAggregator.UserSignalProfile profile,
        List<ProductDTOs.ProductResponse> products
    ) {
        if (products.isEmpty()) return "Here are some of our latest pieces.";

        String topCategory = profile.topBrowsedCategories().isEmpty()
            ? "jewelry" : profile.topBrowsedCategories().get(0);

        return String.format(
            "Based on your interest in %s, we think you'll love these handcrafted pieces.",
            topCategory
        );
    }

    private AIDTOs.AIRecommendationResponse buildFallbackResponse() {
        List<ProductDTOs.ProductResponse> latest = productRepository
            .findTop10ByActiveTrueOrderByCreatedAtDesc()
            .stream()
            .map(productMapper::toResponse)
            .limit(MAX_RECOMMENDATIONS)
            .collect(Collectors.toList());

        return new AIDTOs.AIRecommendationResponse(latest, "Our latest handcrafted pieces, just for you.");
    }

    @SuppressWarnings("unchecked")
    private AIDTOs.AIRecommendationResponse getCached(String key) {
        try {
            Object val = redisTemplate.opsForValue().get(key);
            if (val instanceof AIDTOs.AIRecommendationResponse r) return r;
        } catch (Exception e) {
            log.warn("Redis read failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheResponse(String key, AIDTOs.AIRecommendationResponse response) {
        try {
            redisTemplate.opsForValue().set(key, response, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }
    }
}
