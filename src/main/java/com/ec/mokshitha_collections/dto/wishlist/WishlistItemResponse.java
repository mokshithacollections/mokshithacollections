package com.ec.mokshitha_collections.dto.wishlist;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Flattened view of a wishlist entry for the account page. The image is the
 * saved variant's primary/first image (falling back to the product hero),
 * mirroring how the cart shows variant images.
 */
@Getter
@Builder
public class WishlistItemResponse {
    private final Long wishlistId;
    private final Long productId;
    private final String productName;
    private final BigDecimal price;
    private final BigDecimal discountPrice;
    private final String imageUrl;

    private final Long variantId;
    private final String color;
    private final String size;
}
