package com.ec.mokshitha_collections.dto.product;

import com.ec.mokshitha_collections.dto.category.CategoryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {
    private final Long productId;
    private final String name;
    private final String description;
    private final String sku;
    private final BigDecimal price;
    private final BigDecimal discountPrice;
    private final String imageUrl;
    private final String fabricType;
    private final String occasion;
    private final Boolean isFeatured;
    private final Boolean isBestseller;
    private final Boolean isTrending;
    private final Boolean isActive;
    private final LocalDateTime createdAt;

    private final CategoryResponse category;
    private final List<VariantResponse> variants;
    private final Double averageRating;
    private final Long reviewCount;
}
