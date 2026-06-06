package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.admin.OrderStatusUpdateRequest;
import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.dto.order.OrderResponse;
import com.ec.mokshitha_collections.dto.order.OrderSummaryResponse;
import com.ec.mokshitha_collections.entity.OrderStatus;
import com.ec.mokshitha_collections.service.admin.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService service;

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = service.list(status, pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest req) {
        return ResponseEntity.ok(service.updateStatus(id, req));
    }
}
