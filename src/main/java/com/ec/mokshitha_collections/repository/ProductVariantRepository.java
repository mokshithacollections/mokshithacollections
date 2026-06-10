package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** Remove all variants of a product (hard delete; delete images/cart first). */
    @Modifying
    @Query("DELETE FROM ProductVariant v WHERE v.product.productId = :productId")
    void deleteByProductId(Long productId);

    /**
     * Atomically reserve stock for a variant. The {@code stockQuantity >= :qty}
     * guard means the DB row-lock lets only ONE concurrent caller win the last
     * unit — returns 1 when reserved, 0 when there isn't enough (no oversell).
     */
    @Modifying
    @Query("""
            UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty
            WHERE v.variantId = :variantId AND v.stockQuantity >= :qty
            """)
    int reserveStock(Long variantId, int qty);

    /** Return previously-reserved stock (on payment failure / expiry / cancel). */
    @Modifying
    @Query("""
            UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty
            WHERE v.variantId = :variantId
            """)
    int releaseStock(Long variantId, int qty);

    /** All variants of a product, ordered by stock so the toggle endpoint
     *  picks the most-available default. */
    @Query("""
            SELECT v FROM ProductVariant v
            WHERE v.product.productId = :productId
            ORDER BY v.stockQuantity DESC, v.variantId ASC
            """)
    List<ProductVariant> findByProductId(Long productId);

    /** Distinct colour count per product, for the products on one listing page.
     *  Returns rows of [productId, colourCount]; products with no variants are
     *  simply absent from the result (callers default them to 0). */
    @Query("""
            SELECT v.product.productId, COUNT(DISTINCT v.color)
            FROM ProductVariant v
            WHERE v.product.productId IN :productIds
            GROUP BY v.product.productId
            """)
    List<Object[]> countDistinctColorsByProductIds(List<Long> productIds);

    /** Convenience for the toggle endpoint: the first in-stock variant. */
    default Optional<ProductVariant> findFirstInStockByProductId(Long productId) {
        return findByProductId(productId).stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                .findFirst();
    }
}
