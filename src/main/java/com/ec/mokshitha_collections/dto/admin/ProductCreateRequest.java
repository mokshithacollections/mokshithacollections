package com.ec.mokshitha_collections.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {

    @NotBlank @Size(max = 255)
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be > 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", inclusive = false, message = "Discount price must be > 0")
    private BigDecimal discountPrice;

    @NotBlank @Size(max = 64)
    private String sku;

    private String imageUrl;

    @NotNull
    private Long categoryId;

    private String fabricType;
    private String occasion;

    private Boolean isFeatured;
    private Boolean isBestseller;
    private Boolean isTrending;
    private Boolean isActive;
}
