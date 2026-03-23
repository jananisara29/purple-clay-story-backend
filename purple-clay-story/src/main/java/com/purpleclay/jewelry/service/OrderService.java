package com.purpleclay.jewelry.service;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.CartDTOs;
import com.purpleclay.jewelry.model.entity.*;
import com.purpleclay.jewelry.model.enums.OrderStatus;
import com.purpleclay.jewelry.repository.OrderRepository;
import com.purpleclay.jewelry.repository.ProductRepository;
import com.purpleclay.jewelry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;

    @Transactional
    public CartDTOs.OrderResponse placeOrder(Long userId, CartDTOs.PlaceOrderRequest request) {
        CartDTOs.Cart cart = cartService.getCart(userId);
        if (cart.items().isEmpty()) throw new BadRequestException("Cart is empty");
        if (request.shippingAddress() == null || request.shippingAddress().isBlank())
            throw new BadRequestException("Shipping address is required");

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Validate all stock upfront (atomic)
        for (CartDTOs.CartItem item : cart.items()) {
            Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));
            if (!product.isActive())
                throw new BadRequestException("Product unavailable: " + item.productName());
            if (product.getStockQuantity() < item.quantity())
                throw new BadRequestException("Insufficient stock for: " + item.productName()
                    + ". Available: " + product.getStockQuantity());
        }

        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .user(user)
            .status(OrderStatus.PENDING)
            .totalAmount(cart.computeTotal())
            .shippingAddress(request.shippingAddress())
            .notes(request.notes())
            .orderItems(new ArrayList<>())
            .build();

        for (CartDTOs.CartItem cartItem : cart.items()) {
            Product product = productRepository.findById(cartItem.productId()).get();
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(cartItem.quantity())
                .unitPrice(cartItem.unitPrice())
                .selectedColor(cartItem.selectedColor())
                .selectedShape(cartItem.selectedShape())
                .selectedHookType(cartItem.selectedHookType())
                .customPreviewUrl(cartItem.customPreviewUrl())
                .build();
            order.getOrderItems().add(orderItem);
            productService.decrementStock(product.getId(), cartItem.quantity());
        }

        order = orderRepository.save(order);
        log.info("Order placed: {} for user {}", order.getOrderNumber(), userId);
        cartService.clearCart(userId);
        return toOrderResponse(order);
    }

    public CartDTOs.PagedOrderResponse getMyOrders(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        List<CartDTOs.OrderResponse> responses = orders.getContent().stream()
            .map(this::toOrderResponse).toList();
        return new CartDTOs.PagedOrderResponse(
            responses, orders.getNumber(), orders.getSize(),
            orders.getTotalElements(), orders.getTotalPages()
        );
    }

    public CartDTOs.OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId))
            throw new BadRequestException("Order does not belong to this user");
        return toOrderResponse(order);
    }

    @Transactional
    public CartDTOs.OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        try { order.setStatus(OrderStatus.valueOf(status.toUpperCase())); }
        catch (IllegalArgumentException e) { throw new BadRequestException("Invalid status: " + status); }
        return toOrderResponse(orderRepository.save(order));
    }

    private CartDTOs.OrderResponse toOrderResponse(Order order) {
        List<CartDTOs.OrderItemResponse> items = order.getOrderItems().stream()
            .map(oi -> new CartDTOs.OrderItemResponse(
                oi.getProduct().getId(), oi.getProduct().getName(),
                oi.getUnitPrice(), oi.getQuantity(),
                oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())),
                oi.getSelectedColor(), oi.getSelectedShape(),
                oi.getSelectedHookType(), oi.getCustomPreviewUrl()
            )).toList();

        return new CartDTOs.OrderResponse(
            order.getId(), order.getOrderNumber(), order.getStatus().name(),
            order.getTotalAmount(), order.getShippingAddress(), order.getNotes(),
            items, order.getCreatedAt()
        );
    }

    private String generateOrderNumber() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PCS-" + ts + "-" + suffix;
    }
}
