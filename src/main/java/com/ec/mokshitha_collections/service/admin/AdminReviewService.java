package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.dto.admin.PendingReviewResponse;
import com.ec.mokshitha_collections.entity.ProductReview;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ProductReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Page<PendingReviewResponse> listPending(Pageable pageable) {
        return reviewRepository.findPending(pageable).map(AdminReviewService::toPending);
    }

    @Transactional
    public PendingReviewResponse approve(Long reviewId) {
        ProductReview r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        r.setIsApproved(true);
        return toPending(reviewRepository.save(r));
    }

    @Transactional
    public void reject(Long reviewId) {
        ProductReview r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(r);
    }

    private static PendingReviewResponse toPending(ProductReview r) {
        return PendingReviewResponse.builder()
                .reviewId(r.getReviewId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .productId(r.getProduct().getProductId())
                .productName(r.getProduct().getName())
                .reviewerUserId(r.getUser().getUserId())
                .reviewerEmail(r.getUser().getEmail())
                .reviewerName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                .build();
    }
}
