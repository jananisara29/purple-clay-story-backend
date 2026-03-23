package com.purpleclay.jewelry.service;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.CartDTOs;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;

    private static final String CART_KEY_PREFIX = "cart:user:";
    private static final Duration CART_TTL = Duration.ofDays(7);

    public CartDTOs.Cart getCart(Long userId) {
        CartDTOs.Cart cart = fetchFromRedis(userId);
        return cart != null ? cart : CartDTOs.Cart.empty(userId);
    }

    public CartDTOs.Cart addItem(Long userId, CartDTOs.AddToCartRequest request) {
        if (request.quantity() <= 0) throw new BadRequestException("Quantity must be at least 1");

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        if (!product.isActive()) throw new BadRequestException("Product unavailable: " + product.getName());
        if (product.getStockQuantity() < request.quantity())
            throw new BadRequestException("Only " + product.getStockQuantity() + " in stock for: " + product.getName());

        CartDTOs.Cart cart = getCart(userId);
        List<CartDTOs.CartItem> items = new ArrayList<>(cart.items());

        int existingIdx = findExistingItemIndex(items, request);
        if (existingIdx >= 0) {
            CartDTOs.CartItem existing = items.get(existingIdx);
            int newQty = existing.quantity() + request.quantity();
            if (product.getStockQuantity() < newQty)
                throw new BadRequestException("Cannot add more. Only " + product.getStockQuantity() + " in stock.");
            items.set(existingIdx, withQuantity(existing, newQty));
        } else {
            items.add(new CartDTOs.CartItem(
                product.getId(), product.getName(), product.getImageUrl(),
                product.getPrice(), request.quantity(),
                request.selectedColor(), request.selectedShape(),
                request.selectedHookType(), request.customPreviewUrl()
            ));
        }

        return saveAndReturn(userId, items);
    }

    public CartDTOs.Cart updateItem(Long userId, CartDTOs.UpdateCartItemRequest request) {
        CartDTOs.Cart cart = getCart(userId);
        List<CartDTOs.CartItem> items = new ArrayList<>(cart.items());

        if (request.quantity() == 0) {
            items.removeIf(i -> i.productId().equals(request.productId()));
        } else {
            Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
            if (product.getStockQuantity() < request.quantity())
                throw new BadRequestException("Only " + product.getStockQuantity() + " in stock.");

            boolean found = false;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).productId().equals(request.productId())) {
                    items.set(i, withQuantity(items.get(i), request.quantity()));
                    found = true;
                    break;
                }
            }
            if (!found) throw new BadRequestException("Item not found in cart");
        }

        return saveAndReturn(userId, items);
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(CART_KEY_PREFIX + userId);
        log.info("Cart cleared for user {}", userId);
    }

    private CartDTOs.CartItem withQuantity(CartDTOs.CartItem item, int qty) {
        return new CartDTOs.CartItem(
            item.productId(), item.productName(), item.imageUrl(), item.unitPrice(), qty,
            item.selectedColor(), item.selectedShape(), item.selectedHookType(), item.customPreviewUrl()
        );
    }

    private CartDTOs.Cart saveAndReturn(Long userId, List<CartDTOs.CartItem> items) {
        BigDecimal total = items.stream().map(CartDTOs.CartItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        CartDTOs.Cart updated = new CartDTOs.Cart(userId, items, total);
        try { redisTemplate.opsForValue().set(CART_KEY_PREFIX + userId, updated, CART_TTL); }
        catch (Exception e) { log.error("Redis cart save failed: {}", e.getMessage()); }
        return updated;
    }

    private int findExistingItemIndex(List<CartDTOs.CartItem> items, CartDTOs.AddToCartRequest req) {
        for (int i = 0; i < items.size(); i++) {
            CartDTOs.CartItem item = items.get(i);
            if (item.productId().equals(req.productId())
                && eq(item.selectedColor(), req.selectedColor())
                && eq(item.selectedShape(), req.selectedShape())
                && eq(item.selectedHookType(), req.selectedHookType())) return i;
        }
        return -1;
    }

    private boolean eq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private CartDTOs.Cart fetchFromRedis(Long userId) {
        try {
            Object val = redisTemplate.opsForValue().get(CART_KEY_PREFIX + userId);
            if (val instanceof CartDTOs.Cart c) return c;
        } catch (Exception e) { log.warn("Redis cart read failed: {}", e.getMessage()); }
        return null;
    }
}
