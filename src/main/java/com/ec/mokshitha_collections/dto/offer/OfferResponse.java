package com.ec.mokshitha_collections.dto.offer;

import com.ec.mokshitha_collections.entity.OfferDiscountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfferResponse {
    private final Long offerId;
    private final String name;
    private final String code;
    private final String description;
    private final OfferDiscountType discountType;
    private final BigDecimal discountValue;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final BigDecimal minOrderAmount;
    private final BigDecimal maxDiscountAmount;
    private final Integer perCustomerLimit;
    private final Boolean active;
    private final Boolean autoApply;
    private final Integer priority;
    private final Set<Long> productIds;
    private final Set<Long> categoryIds;

    /** Derived for the admin UI: SCHEDULED | LIVE | EXPIRED | INACTIVE. */
    private final String state;
}
