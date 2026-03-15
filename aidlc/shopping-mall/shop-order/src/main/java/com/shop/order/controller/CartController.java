package com.shop.order.controller;

import com.shop.order.dto.CartItemRequest;
import com.shop.order.dto.CartResponse;
import com.shop.order.filter.JwtAuthFilter.UserPrincipal;
import com.shop.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.userId()));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                        @Valid @RequestBody CartItemRequest request) {
        cartService.addItem(principal.userId(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Void> updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long productId,
                                           @Valid @RequestBody CartItemRequest request) {
        cartService.updateItem(principal.userId(), productId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long productId) {
        cartService.removeItem(principal.userId(), productId);
        return ResponseEntity.noContent().build();
    }
}
