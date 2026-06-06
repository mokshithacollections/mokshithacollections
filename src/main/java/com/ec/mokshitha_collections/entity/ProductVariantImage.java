package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductVariantImage {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "view_type", nullable = false)
    private String viewType; // FRONT, BACK, SIDE

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;
}
