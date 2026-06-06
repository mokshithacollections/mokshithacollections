package com.ec.mokshitha_collections.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {
    private final Long cartItemId;
    private final Long productId;
    private final String productName;
    private final String imageUrl;
    private final Long variantId;
    private final String color;
    private final String size;
    private final BigDecimal unitPrice;
    private final Integer quantity;
    private final BigDecimal lineTotal;
    private final Integer stockAvailable;
}
