package com.purpleclay.jewelry.model.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cart is stored entirely in Redis — no DB table.
 * TTL: 7 days. On checkout, cart is cleared.
 */
public class Cart implements Serializable {

    private Long userId;
    private List<CartItem> items = new ArrayList<>();

    public Cart() {}

    public Cart(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public void addOrUpdate(CartItem newItem) {
        // If same product + same customization, increment quantity
        items.stream()
            .filter(i -> i.getProductId().equals(newItem.getProductId())
                && eq(i.getSelectedColor(), newItem.getSelectedColor())
                && eq(i.getSelectedShape(), newItem.getSelectedShape())
                && eq(i.getSelectedHookType(), newItem.getSelectedHookType()))
            .findFirst()
            .ifPresentOrElse(
                existing -> existing.setQuantity(existing.getQuantity() + newItem.getQuantity()),
                () -> {
                    newItem.setCartItemId(UUID.randomUUID().toString());
                    items.add(newItem);
                }
            );
    }

    public void removeItem(String cartItemId) {
        items.removeIf(i -> i.getCartItemId().equals(cartItemId));
    }

    public void updateQuantity(String cartItemId, int quantity) {
        items.stream()
            .filter(i -> i.getCartItemId().equals(cartItemId))
            .findFirst()
            .ifPresent(i -> i.setQuantity(quantity));
    }

    public void clear() {
        items.clear();
    }

    public BigDecimal totalAmount() {
        return items.stream()
            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int totalItems() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    private boolean eq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ─── Inner class ──────────────────────────────────────────────────────────

    public static class CartItem implements Serializable {
        private String cartItemId;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private BigDecimal unitPrice;
        private Integer quantity;
        private String selectedColor;
        private String selectedShape;
        private String selectedHookType;
        private String customPreviewUrl;

        public CartItem() {}

        // Getters & Setters
        public String getCartItemId() { return cartItemId; }
        public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductImageUrl() { return productImageUrl; }
        public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getSelectedColor() { return selectedColor; }
        public void setSelectedColor(String selectedColor) { this.selectedColor = selectedColor; }
        public String getSelectedShape() { return selectedShape; }
        public void setSelectedShape(String selectedShape) { this.selectedShape = selectedShape; }
        public String getSelectedHookType() { return selectedHookType; }
        public void setSelectedHookType(String selectedHookType) { this.selectedHookType = selectedHookType; }
        public String getCustomPreviewUrl() { return customPreviewUrl; }
        public void setCustomPreviewUrl(String customPreviewUrl) { this.customPreviewUrl = customPreviewUrl; }
    }
}
