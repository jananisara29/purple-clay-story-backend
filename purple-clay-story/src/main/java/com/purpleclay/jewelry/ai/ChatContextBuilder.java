package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.CategoryRepository;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    private static final int MAX_PRODUCTS_IN_CONTEXT = 20;

    /**
     * Builds the system prompt injected into every chat call.
     * Cached for 10 minutes — product catalog doesn't change every second.
     */
    @Cacheable("chatContext")
    public String buildSystemPrompt() {
        String catalogSummary = buildCatalogSummary();

        return """
            You are Priya, the friendly AI shopping assistant for "Purple Clay Story" — a handmade cold porcelain clay jewelry brand based in Chennai, India.

            YOUR PERSONALITY:
            - Warm, knowledgeable, and enthusiastic about handmade jewelry
            - Speak in a friendly, conversational tone (mix of English is fine)
            - Never robotic. Never list-heavy. Converse naturally.

            WHAT YOU CAN HELP WITH:
            - Recommending jewelry based on occasion, budget, preference, or style
            - Explaining what cold porcelain clay jewelry is and how it's made
            - Guiding customers through customization options (color, shape, hook type)
            - Answering questions about care, durability, and gifting
            - Helping customers find the right product from the catalog

            WHAT YOU CANNOT DO:
            - Process orders or payments (tell them to use the cart)
            - Access real-time stock (tell them to check the product page)
            - Make up products not in the catalog

            CURRENT PRODUCT CATALOG:
            """ + catalogSummary + """

            IMPORTANT RULES:
            - Only recommend products from the catalog above
            - If asked about a product not in the catalog, say it's not currently available
            - If budget is mentioned, only suggest products within range
            - Always end with a follow-up question or gentle call to action
            - Keep replies under 150 words unless the customer asks for more detail
            """;
    }

    private String buildCatalogSummary() {
        List<Product> products = productRepository.findTop10ByActiveTrueOrderByCreatedAtDesc();

        if (products.isEmpty()) {
            return "No products currently available.";
        }

        return products.stream()
            .map(p -> String.format("- %s | ₹%.0f | Category: %s | Customizable: %s%s",
                p.getName(),
                p.getPrice(),
                p.getCategory().getName(),
                p.isCustomizable() ? "Yes" : "No",
                p.getAiDescription() != null
                    ? " | " + truncate(p.getAiDescription(), 80)
                    : ""
            ))
            .collect(Collectors.joining("\n"));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
