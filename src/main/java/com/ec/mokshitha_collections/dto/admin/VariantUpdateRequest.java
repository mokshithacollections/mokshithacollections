package com.ec.mokshitha_collections.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VariantUpdateRequest {

    @Size(max = 64)
    private String color;

    @Size(max = 32)
    private String size;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @Size(max = 64)
    private String skuVariant;
}
