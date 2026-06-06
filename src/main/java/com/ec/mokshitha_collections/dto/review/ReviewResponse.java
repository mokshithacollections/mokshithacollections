package com.ec.mokshitha_collections.dto.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {
    private final Long reviewId;
    private final Integer rating;
    private final String comment;
    private final LocalDateTime createdAt;
    /** First name only — never expose email/lastName/etc to other shoppers. */
    private final String reviewerFirstName;
    private final boolean ownReview;
}
