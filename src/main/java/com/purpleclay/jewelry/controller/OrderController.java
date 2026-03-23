package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.CartDTOs;
import com.purpleclay.jewelry.repository.UserRepository;
import com.purpleclay.jewelry.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place and manage orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Place order from current cart — clears cart on success")
    public ResponseEntity<CartDTOs.OrderResponse> placeOrder(
        @RequestBody CartDTOs.PlaceOrderRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.placeOrder(resolveUserId(userDetails), request));
    }

    @GetMapping
    @Operation(summary = "Get current user's order history (paginated)")
    public ResponseEntity<CartDTOs.PagedOrderResponse> getMyOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(resolveUserId(userDetails), page, size));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get a specific order by ID (own orders only)")
    public ResponseEntity<CartDTOs.OrderResponse> getOrder(
        @PathVariable Long orderId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.getOrderById(orderId, resolveUserId(userDetails)));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin only)")
    public ResponseEntity<CartDTOs.OrderResponse> updateStatus(
        @PathVariable Long orderId,
        @RequestParam String status
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            .getId();
    }
}
