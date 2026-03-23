package com.purpleclay.jewelry.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartDTOs {

    // ─── Cart item (stored in Redis as part of Cart) ─────────────────────────

    public record CartItem(
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        String selectedColor,
        String selectedShape,
        String selectedHookType,
        String customPreviewUrl
    ) {
        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    // ─── Full cart (Redis value) ──────────────────────────────────────────────

    public record Cart(
        Long userId,
        List<CartItem> items,
        BigDecimal totalAmount
    ) {
        public static Cart empty(Long userId) {
            return new Cart(userId, new ArrayList<>(), BigDecimal.ZERO);
        }

        public BigDecimal computeTotal() {
            return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    // ─── Requests ─────────────────────────────────────────────────────────────

    public record AddToCartRequest(
        Long productId,
        int quantity,
        String selectedColor,
        String selectedShape,
        String selectedHookType,
        String customPreviewUrl
    ) {}

    public record UpdateCartItemRequest(
        Long productId,
        int quantity
    ) {}

    // ─── Order Requests ───────────────────────────────────────────────────────

    public record PlaceOrderRequest(
        String shippingAddress,
        String notes
    ) {}

    // ─── Order Responses ──────────────────────────────────────────────────────

    public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        String selectedColor,
        String selectedShape,
        String selectedHookType,
        String customPreviewUrl
    ) {}

    public record OrderResponse(
        Long id,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        String shippingAddress,
        String notes,
        List<OrderItemResponse> items,
        java.time.LocalDateTime createdAt
    ) {}

    public record PagedOrderResponse(
        List<OrderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {}
}
