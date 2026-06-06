package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"product_id", "color", "size"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String color;

    @Column
    private String size;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "sku_variant", unique = true)
    private String skuVariant;
}
