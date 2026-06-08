package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /** Remove all reviews for a product (hard delete). */
    @Modifying
    @Query("DELETE FROM ProductReview r WHERE r.product.productId = :productId")
    void deleteByProductId(Long productId);


    @Query("""
            SELECT r FROM ProductReview r
            JOIN FETCH r.user
            WHERE r.product.productId = :productId AND r.isApproved = true
            ORDER BY r.createdAt DESC
            """)
    Page<ProductReview> findApprovedByProductId(Long productId, Pageable pageable);

    Optional<ProductReview> findByProductProductIdAndUserUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.productId = :productId AND r.isApproved = true")
    Double averageRatingForProduct(Long productId);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.productId = :productId AND r.isApproved = true")
    long countApprovedForProduct(Long productId);

    /** Admin: list reviews awaiting approval. */
    @Query("""
            SELECT r FROM ProductReview r
            JOIN FETCH r.user
            JOIN FETCH r.product
            WHERE r.isApproved = false
            ORDER BY r.createdAt ASC
            """)
    Page<ProductReview> findPending(Pageable pageable);
}
