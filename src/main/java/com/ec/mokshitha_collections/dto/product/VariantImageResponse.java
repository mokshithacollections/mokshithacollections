package com.ec.mokshitha_collections.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariantImageResponse {
    private final Long imageId;
    private final String imageUrl;
    private final String viewType;
    private final Boolean isPrimary;
}
