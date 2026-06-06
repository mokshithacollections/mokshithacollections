package com.ec.mokshitha_collections.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    private final Long cartId;
    private final List<CartItemResponse> items;
    /** Number of distinct lines (badge in the header). */
    private final int itemCount;
    /** Sum of quantities across all lines. */
    private final int totalQuantity;
    private final BigDecimal subtotal;
}
