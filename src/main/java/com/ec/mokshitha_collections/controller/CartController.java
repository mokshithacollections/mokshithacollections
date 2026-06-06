package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.cart.AddCartItemRequest;
import com.ec.mokshitha_collections.dto.cart.CartResponse;
import com.ec.mokshitha_collections.dto.cart.UpdateCartItemRequest;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @ModelAttribute AddCartItemRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.addItem(
                principal.getUserId(), req.getVariantId(), req.getQuantity()));
    }

    @PostMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable("id") Long cartItemId,
            @Valid @ModelAttribute UpdateCartItemRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.updateQuantity(
                principal.getUserId(), cartItemId, req.getQuantity()));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<CartResponse> deleteItem(
            @PathVariable("id") Long cartItemId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.removeItem(principal.getUserId(), cartItemId));
    }

    /**
     * Frontend-compatible toggle (home.js / account.js call this from product
     * cards). Adds qty=1 of the first in-stock variant if not already in cart;
     * otherwise removes any existing lines for the product.
     */
    @PostMapping("/toggle/{productId}")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        var result = cartService.toggleProduct(principal.getUserId(), productId);
        return ResponseEntity.ok(Map.of(
                "added", result.added(),
                "totalQuantity", result.cart().getTotalQuantity(),
                "itemCount", result.cart().getItemCount()));
    }
}
