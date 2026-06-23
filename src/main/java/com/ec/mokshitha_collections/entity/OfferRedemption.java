package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One record per successful use of an offer by a customer. Used to enforce an
 * offer's per-customer usage limit and to audit which order a discount was
 * applied to. Written at payment confirmation (never at checkout reserve).
 */
@Entity
@Table(name = "offer_redemptions", indexes = {
        @Index(name = "idx_redemption_offer_user", columnList = "offer_id,user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "redemption_id")
    private Long redemptionId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(length = 40)
    private String code;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    @PrePersist
    void onCreate() {
        if (redeemedAt == null) redeemedAt = LocalDateTime.now();
    }
}
