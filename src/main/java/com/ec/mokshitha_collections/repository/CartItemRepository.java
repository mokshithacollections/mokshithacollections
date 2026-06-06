package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartCartIdAndVariantVariantId(Long cartId, Long variantId);

    long countByCartCartId(Long cartId);

    /** Returns the distinct product ids currently in this user's cart — used
     *  by GlobalModelAdvice so product cards can render "View Cart" instead
     *  of "Add to Cart" when the product is already in the cart. */
    @Query("""
            SELECT DISTINCT v.product.productId
            FROM CartItem ci
            JOIN ci.variant v
            JOIN ci.cart c
            WHERE c.user.userId = :userId
            """)
    List<Long> findProductIdsByUserId(Long userId);

    /** Variant ids currently in this user's cart — lets the product-detail page
     *  flip its button to "View Cart" for the exact variant that's in the cart. */
    @Query("""
            SELECT DISTINCT v.variantId
            FROM CartItem ci
            JOIN ci.variant v
            JOIN ci.cart c
            WHERE c.user.userId = :userId
            """)
    List<Long> findVariantIdsByUserId(Long userId);
}
