package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.repository.BrowseEventRepository;
import com.purpleclay.jewelry.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSignalAggregator {

    private final BrowseEventRepository browseEventRepository;
    private final OrderRepository orderRepository;

    private static final int BROWSE_LOOKBACK_DAYS = 30;

    /**
     * Builds a text summary of user behavior for GPT-4 context injection.
     */
    public UserSignalProfile buildProfile(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(BROWSE_LOOKBACK_DAYS);

        // Browse signals
        List<String> recentlyViewedNames = browseEventRepository
            .findRecentByUserId(userId, since)
            .stream()
            .map(b -> b.getProductName() + " (" + b.getCategoryName() + ")")
            .distinct()
            .limit(10)
            .collect(Collectors.toList());

        List<String> topBrowsedCategories = browseEventRepository
            .findTopCategoriesByUserId(userId)
            .stream()
            .limit(3)
            .map(row -> (String) row[0])
            .collect(Collectors.toList());

        List<Long> alreadyViewedIds = browseEventRepository
            .findMostViewedProductIds(userId);

        // Purchase signals
        List<String> purchasedCategories = orderRepository
            .findPurchasedCategoriesByUserId(userId);

        List<Long> purchasedProductIds = orderRepository
            .findPurchasedProductIdsByUserId(userId);

        List<String> purchaseHistory = orderRepository
            .findPurchaseHistorySummary(userId)
            .stream()
            .limit(10)
            .map(row -> row[0] + " in " + row[1])
            .collect(Collectors.toList());

        return new UserSignalProfile(
            recentlyViewedNames,
            topBrowsedCategories,
            alreadyViewedIds,
            purchasedCategories,
            purchasedProductIds,
            purchaseHistory
        );
    }

    public record UserSignalProfile(
        List<String> recentlyViewed,
        List<String> topBrowsedCategories,
        List<Long> viewedProductIds,
        List<String> purchasedCategories,
        List<Long> purchasedProductIds,
        List<String> purchaseHistory
    ) {
        public boolean isEmpty() {
            return recentlyViewed.isEmpty() && purchaseHistory.isEmpty();
        }

        /** Combines viewed + purchased IDs to exclude from recommendations */
        public Set<Long> excludeProductIds() {
            Set<Long> exclude = new HashSet<>(purchasedProductIds);
            exclude.addAll(viewedProductIds.stream().limit(5).toList());
            return exclude;
        }
    }
}
