package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.service.ProductService;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIDescriptionService {

    private final OpenAIClient openAIClient;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ProductMapper productMapper;

    private static final String SYSTEM_PROMPT = """
        You are a luxury jewelry copywriter for "Purple Clay Story", a handmade cold porcelain clay jewelry brand.
        Your descriptions are:
        - Warm, elegant, and emotionally resonant
        - 2-3 sentences max
        - Highlight craftsmanship, uniqueness, and material
        - Never use generic phrases like "perfect gift" or "beautiful piece"
        - Always mention the material: cold porcelain clay
        - End with a subtle sensory detail (texture, weight, feel)
        Return ONLY the description text. No quotes. No extra formatting.
        """;

    /**
     * Generate AI description for a single product.
     * Saves it to DB and returns the generated text.
     */
    public String generateForProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        String userMessage = buildUserPrompt(product);
        log.info("Generating AI description for product: {}", product.getName());

        String aiDescription = openAIClient.chat(SYSTEM_PROMPT, userMessage);
        aiDescription = aiDescription.trim();

        productService.updateAiDescription(productId, aiDescription);
        log.info("AI description saved for product: {}", product.getName());

        return aiDescription;
    }

    /**
     * Async batch generation for all products missing AI descriptions.
     * Call this as admin to bulk-generate.
     */
    @Async
    public void generateForAllMissing() {
        List<Product> products = productRepository.findAll()
            .stream()
            .filter(p -> p.isActive() && (p.getAiDescription() == null || p.getAiDescription().isBlank()))
            .toList();

        log.info("Starting batch AI description generation for {} products", products.size());

        for (Product product : products) {
            try {
                generateForProduct(product.getId());
                // Small delay to avoid rate limiting
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Failed to generate description for product {}: {}", product.getId(), e.getMessage());
            }
        }

        log.info("Batch AI description generation complete");
    }

    /**
     * Regenerate — forces a fresh description even if one exists.
     */
    public String regenerateForProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        String userMessage = buildUserPrompt(product);
        log.info("Regenerating AI description for product: {}", product.getName());

        String aiDescription = openAIClient.chat(SYSTEM_PROMPT, userMessage).trim();
        productService.updateAiDescription(productId, aiDescription);

        return aiDescription;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String buildUserPrompt(Product product) {
        StringBuilder sb = new StringBuilder();
        sb.append("Product name: ").append(product.getName()).append("\n");
        sb.append("Category: ").append(product.getCategory().getName()).append("\n");
        sb.append("Price: ₹").append(product.getPrice()).append("\n");

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            sb.append("Base description: ").append(product.getDescription()).append("\n");
        }

        List<String> colors = productMapper.parseJsonList(product.getAvailableColors());
        if (!colors.isEmpty()) {
            sb.append("Available colors: ").append(String.join(", ", colors)).append("\n");
        }

        List<String> shapes = productMapper.parseJsonList(product.getAvailableShapes());
        if (!shapes.isEmpty()) {
            sb.append("Shapes: ").append(String.join(", ", shapes)).append("\n");
        }

        if (product.isCustomizable()) {
            sb.append("This product is customizable by the customer.\n");
        }

        sb.append("\nWrite a compelling product description for this jewelry piece.");
        return sb.toString();
    }
}
