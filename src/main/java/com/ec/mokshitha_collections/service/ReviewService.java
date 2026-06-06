package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.review.ReviewRequest;
import com.ec.mokshitha_collections.dto.review.ReviewResponse;
import com.ec.mokshitha_collections.entity.Product;
import com.ec.mokshitha_collections.entity.ProductReview;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.ConflictException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductRepository;
import com.ec.mokshitha_collections.repository.ProductReviewRepository;
import com.ec.mokshitha_collections.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listForProduct(Long productId, Long currentUserId, Pageable pageable) {
        return reviewRepository.findApprovedByProductId(productId, pageable)
                .map(r -> toResponse(r, currentUserId));
    }

    @Transactional
    public ReviewResponse addReview(Long productId, Long userId, ReviewRequest req) {
        if (reviewRepository.findByProductProductIdAndUserUserId(productId, userId).isPresent()) {
            throw new ConflictException("You have already reviewed this product");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(req.getRating())
                .comment(req.getComment())
                // Reviews need admin approval before they show on the public detail page.
                // Set to true here if you want auto-approval — or build admin tooling in Phase 6.
                .isApproved(false)
                .build();
        ProductReview saved = reviewRepository.save(review);
        return toResponse(saved, userId);
    }

    @Transactional
    public ReviewResponse updateOwn(Long reviewId, Long userId, ReviewRequest req) {
        ProductReview review = loadOwned(reviewId, userId);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        // Edits invalidate the previous approval — admin re-reviews.
        review.setIsApproved(false);
        return toResponse(reviewRepository.save(review), userId);
    }

    @Transactional
    public void deleteOwn(Long reviewId, Long userId) {
        ProductReview review = loadOwned(reviewId, userId);
        reviewRepository.delete(review);
    }

    private ProductReview loadOwned(Long reviewId, Long userId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this review");
        }
        return review;
    }

    private static ReviewResponse toResponse(ProductReview r, Long currentUserId) {
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .reviewerFirstName(r.getUser().getFirstName())
                .ownReview(currentUserId != null && currentUserId.equals(r.getUser().getUserId()))
                .build();
    }
}
