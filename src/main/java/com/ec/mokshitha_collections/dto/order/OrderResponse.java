package com.ec.mokshitha_collections.dto.order;

import com.ec.mokshitha_collections.entity.OrderStatus;
import com.ec.mokshitha_collections.entity.PaymentMethod;
import com.ec.mokshitha_collections.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus status;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus paymentStatus;
    private final BigDecimal subtotal;
    private final BigDecimal shippingFee;
    private final BigDecimal totalAmount;
    private final LocalDateTime placedAt;
    private final LocalDateTime updatedAt;

    /* ---------- Per-status timestamps (filled when admin progresses status) ---------- */
    private final LocalDateTime confirmedAt;
    private final LocalDateTime processingAt;
    private final LocalDateTime shippedAt;
    private final LocalDateTime deliveredAt;
    private final LocalDateTime cancelledAt;

    /* ---------- Tracking info (admin sets when status moves to SHIPPED) ---------- */
    private final String trackingNumber;
    private final String courier;
    private final String trackingUrl;
    private final LocalDate expectedDeliveryDate;

    private final AddressSnapshotResponse shippingAddress;
    private final List<OrderItemResponse> items;
}
