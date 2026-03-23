package com.purpleclay.jewelry.model.dto;

import com.purpleclay.jewelry.model.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDTOs {

    public record PlaceOrderRequest(
        @NotBlank(message = "Shipping address is required")
        String shippingAddress,

        String notes
    ) {}

    public record OrderItemResponse(
        Long productId,
        String productName,
        String productImageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String selectedColor,
        String selectedShape,
        String selectedHookType,
        String customPreviewUrl
    ) {}

    public record OrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        String shippingAddress,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}

    public record UpdateOrderStatusRequest(
        OrderStatus status
    ) {}

    public record PagedOrderResponse(
        List<OrderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
    ) {}
}
