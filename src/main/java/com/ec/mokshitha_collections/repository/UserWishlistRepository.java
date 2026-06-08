package com.ec.mokshitha_collections.repository;

import java.util.List;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ec.mokshitha_collections.entity.UserWishlist;

public interface UserWishlistRepository extends JpaRepository<UserWishlist, Long>{

	/** Remove every wishlist row for a product (hard delete). */
	@Modifying
	@Query("DELETE FROM UserWishlist w WHERE w.product.productId = :productId")
	void deleteByProductId(Long productId);

	/** Wishlist is now keyed on the variant, so the same product can appear
	 *  in several colours. */
	Optional<UserWishlist> findByUserUserIdAndVariantVariantId(Long userId, Long variantId);

    long countByUserUserId(Long userId);

    /** Distinct product ids in the wishlist — drives the filled/unfilled heart
     *  on product cards (a product's heart is filled if any of its variants is
     *  wishlisted). */
    @Query("SELECT DISTINCT w.product.productId FROM UserWishlist w WHERE w.user.userId = :userId")
    List<Long> findProductIdsByUserId(Long userId);

    /** Variant ids in the wishlist — drives the per-variant heart on the
     *  product-detail page and the "already added" markers in the picker. */
    @Query("SELECT w.variant.variantId FROM UserWishlist w WHERE w.user.userId = :userId")
    List<Long> findVariantIdsByUserId(Long userId);

    /** Wishlist rows with variant + product eagerly loaded for the account page. */
    @Query("SELECT w FROM UserWishlist w " +
           "JOIN FETCH w.variant v " +
           "JOIN FETCH v.product p " +
           "WHERE w.user.userId = :userId " +
           "AND p.isActive = true " +
           "ORDER BY w.addedDate DESC")
    List<UserWishlist> findByUserUserIdWithVariant(@Param("userId") Long userId);
}
