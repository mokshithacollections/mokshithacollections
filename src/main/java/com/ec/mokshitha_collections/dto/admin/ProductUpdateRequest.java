package com.ec.mokshitha_collections.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * All fields optional — null = "leave unchanged". Lets the admin patch a
 * single field without resending the rest of the product.
 */
@Data
public class ProductUpdateRequest {

    @Size(max = 255)
    private String name;

    private String description;

    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be > 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", inclusive = false, message = "Discount price must be > 0")
    private BigDecimal discountPrice;

    @Size(max = 64)
    private String sku;

    private String imageUrl;
    private Long categoryId;
    private String fabricType;
    private String occasion;
    private Boolean isFeatured;
    private Boolean isBestseller;
    private Boolean isTrending;
    private Boolean isActive;
}
