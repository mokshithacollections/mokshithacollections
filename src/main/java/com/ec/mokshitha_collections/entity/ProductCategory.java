package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProductCategory parent;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * True = a "group" category that only holds other categories (shown in the
     * parent dropdown, hidden from the product category dropdown). False/null =
     * a normal leaf category that products are assigned to. Nullable so adding
     * the column to an existing table doesn't require a default; null = leaf.
     */
    @Column(name = "is_parent")
    private Boolean isParent;
}
