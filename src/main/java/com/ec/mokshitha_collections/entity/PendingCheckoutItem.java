package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A reserved line within a {@link PendingCheckout}. Mirrors {@link OrderItem}'s
 * snapshot fields so the order can be built verbatim once payment succeeds.
 */
@Entity
@Table(name = "pending_checkout_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingCheckoutItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_item_id")
    private Long pendingItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_id", nullable = false)
    private PendingCheckout pendingCheckout;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_image_url", columnDefinition = "TEXT")
    private String productImageUrl;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "variant_color")
    private String variantColor;

    @Column(name = "variant_size")
    private String variantSize;

    @Column(name = "sku_variant")
    private String skuVariant;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;
}
