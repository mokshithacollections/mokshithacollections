package com.ec.mokshitha_collections.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariantResponse {
    private final Long variantId;
    private final String color;
    private final String size;
    private final Integer stockQuantity;
    private final String skuVariant;
    private final List<VariantImageResponse> images;
}
