package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.dto.order.OrderResponse;
import com.ec.mokshitha_collections.dto.order.OrderSummaryResponse;
import com.ec.mokshitha_collections.dto.order.PlaceOrderRequest;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @ModelAttribute PlaceOrderRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.placeOrder(principal.getUserId(), req));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 10) Pageable pageable) {
        var page = orderService.listForUser(principal.getUserId(), pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(
            @PathVariable("id") Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.getById(principal.getUserId(), orderId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable("id") Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.cancelOrder(principal.getUserId(), orderId));
    }
}
