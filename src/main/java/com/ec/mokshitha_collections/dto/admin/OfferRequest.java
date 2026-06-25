package com.ec.mokshitha_collections.dto.admin;

import com.ec.mokshitha_collections.entity.OfferDiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Admin create/update payload for an offer (JSON). */
@Data
public class OfferRequest {

    @NotBlank @Size(max = 120)
    private String name;

    @NotBlank @Size(max = 40)
    private String code;

    private String description;

    @NotNull
    private OfferDiscountType discountType;

    @NotNull @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime startAt;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime endAt;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    private Integer perCustomerLimit;

    private Boolean active;

    /** Auto-apply the best eligible offer at checkout (no code typing needed). */
    private Boolean autoApply;

    private Integer priority;

    /** Empty = applies to all products. */
    private List<Long> productIds;

    /** Empty = applies to all categories. */
    private List<Long> categoryIds;
}
