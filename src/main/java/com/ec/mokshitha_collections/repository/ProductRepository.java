package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    /** Latest 4 featured & active products. Used by the home page hero strip. */
    List<Product> findTop4ByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();

    boolean existsBySkuIgnoreCase(String sku);

    /** Eager-fetch a product with its category for detail pages. */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.productId = :productId AND p.isActive = true
            """)
    Optional<Product> findActiveByIdWithCategory(Long productId);

    /** Same as above but ignores the active flag — for admin screens that must
     *  open deactivated products (e.g. to re-activate them). */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.productId = :productId
            """)
    Optional<Product> findByIdWithCategory(Long productId);
}
