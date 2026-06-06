package com.ec.mokshitha_collections.dto.order;

import com.ec.mokshitha_collections.entity.OrderStatus;
import com.ec.mokshitha_collections.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryResponse {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus status;
    private final PaymentStatus paymentStatus;
    private final BigDecimal totalAmount;
    private final int itemCount;
    private final LocalDateTime placedAt;
}
