package com.ec.mokshitha_collections.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {
    private final Long productId;
    private final String name;
    private final String sku;
    private final BigDecimal price;
    private final BigDecimal discountPrice;
    private final String imageUrl;
    private final String categoryName;
    private final String categorySlug;
    private final Boolean isFeatured;
    private final Boolean isBestseller;
    private final Boolean isTrending;
    private final Boolean isActive;
    /** Number of distinct colours this product is available in (0 if no variants). */
    private final Integer colorCount;
}
