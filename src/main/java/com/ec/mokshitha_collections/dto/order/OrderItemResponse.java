package com.ec.mokshitha_collections.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemResponse {
    private final Long orderItemId;
    private final Long productId;
    private final String productName;
    private final String productImageUrl;
    private final Long variantId;
    private final String variantColor;
    private final String variantSize;
    private final BigDecimal unitPrice;
    private final Integer quantity;
    private final BigDecimal lineTotal;
}
