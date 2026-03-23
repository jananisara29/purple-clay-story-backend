package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.CartDTOs;
import com.purpleclay.jewelry.repository.UserRepository;
import com.purpleclay.jewelry.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Redis-backed shopping cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<CartDTOs.Cart> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCart(resolveUserId(userDetails)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart (supports customization options)")
    public ResponseEntity<CartDTOs.Cart> addItem(
        @RequestBody CartDTOs.AddToCartRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(cartService.addItem(resolveUserId(userDetails), request));
    }

    @PatchMapping("/items")
    @Operation(summary = "Update item quantity (quantity=0 removes the item)")
    public ResponseEntity<CartDTOs.Cart> updateItem(
        @RequestBody CartDTOs.UpdateCartItemRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(cartService.updateItem(resolveUserId(userDetails), request));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(resolveUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            .getId();
    }
}
