package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.ProductVariantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {

    /** All images for every variant of a given product, primary first. */
    @Query("""
            SELECT i FROM ProductVariantImage i
            WHERE i.variant.product.productId = :productId
            ORDER BY i.isPrimary DESC, i.imageId ASC
            """)
    List<ProductVariantImage> findByProductId(Long productId);

    List<ProductVariantImage> findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(Long variantId);
}
