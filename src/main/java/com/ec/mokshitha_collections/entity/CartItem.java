package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"cart_id", "variant_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "cart_item_id")
	    private Long cartItemId;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "cart_id", nullable = false)
	    private UserCart cart;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "variant_id", nullable = false)
	    private ProductVariant variant;

	    @Column(nullable = false)
	    private Integer quantity;
}
