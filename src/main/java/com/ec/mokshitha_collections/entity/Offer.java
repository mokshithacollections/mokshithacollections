package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A promotional offer / coupon. Customers redeem it by entering its {@code code}
 * at checkout. The discount is item-scoped: when product/category restrictions
 * are set, only the matching cart lines are discounted (empty = whole order).
 */
@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offer_id")
    private Long offerId;

    @Column(nullable = false, length = 120)
    private String name;

    /** Redemption code, stored uppercase; matched case-insensitively. */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 16)
    private OfferDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /** Cart subtotal must be at least this to use the code (null = no minimum). */
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    /** Caps the computed discount, mainly for percentage offers (null = no cap). */
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** How many times one customer may use it (null = unlimited). */
    @Column(name = "per_customer_limit")
    private Integer perCustomerLimit;

    @Column(nullable = false)
    private Boolean active;

    /** Tie-breaker / ordering when several offers could apply (higher = first). */
    @Column(nullable = false)
    private Integer priority;

    /** Restrict to these products (empty = all products). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "offer_products", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<Long> productIds = new HashSet<>();

    /** Restrict to these categories (empty = all categories). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "offer_categories", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "category_id")
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = true;
        if (priority == null) priority = 0;
    }
}
