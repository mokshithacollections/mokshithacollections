package com.ec.mokshitha_collections.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Admin-only view: includes the reviewer's email + product link so the
 *  moderator has enough context to approve or reject. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingReviewResponse {
    private final Long reviewId;
    private final Integer rating;
    private final String comment;
    private final LocalDateTime createdAt;
    private final Long productId;
    private final String productName;
    private final Long reviewerUserId;
    private final String reviewerEmail;
    private final String reviewerName;
}
