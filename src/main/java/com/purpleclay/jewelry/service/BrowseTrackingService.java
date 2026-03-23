package com.purpleclay.jewelry.service;

import com.purpleclay.jewelry.ai.AIRecommendationService;
import com.purpleclay.jewelry.model.entity.BrowseEvent;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.BrowseEventRepository;
import com.purpleclay.jewelry.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrowseTrackingService {

    private final BrowseEventRepository browseEventRepository;
    private final ProductRepository productRepository;
    private final AIRecommendationService recommendationService;

    /**
     * Fire-and-forget browse event tracking.
     * Called from ProductController on GET /products/{id} for logged-in users.
     */
    @Async
    public void track(Long userId, Long productId) {
        try {
            productRepository.findById(productId).ifPresent(product -> {
                BrowseEvent event = BrowseEvent.builder()
                    .userId(userId)
                    .productId(productId)
                    .productName(product.getName())
                    .categoryName(product.getCategory().getName())
                    .build();
                browseEventRepository.save(event);
                recommendationService.trackView(userId, product);
                log.debug("Browse tracked: user={} product={}", userId, productId);
            });
        } catch (Exception e) {
            log.warn("Browse tracking failed silently: {}", e.getMessage());
        }
    }
}
