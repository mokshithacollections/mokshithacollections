package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.dto.review.ReviewRequest;
import com.ec.mokshitha_collections.dto.review.ReviewResponse;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = principal != null ? principal.getUserId() : null;
        var page = reviewService.listForProduct(productId, currentUserId, pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> add(
            @PathVariable Long productId,
            @Valid @ModelAttribute ReviewRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reviewService.addReview(productId, principal.getUserId(), req));
    }

    @PostMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Valid @ModelAttribute ReviewRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reviewService.updateOwn(reviewId, principal.getUserId(), req));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        reviewService.deleteOwn(reviewId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }
}
