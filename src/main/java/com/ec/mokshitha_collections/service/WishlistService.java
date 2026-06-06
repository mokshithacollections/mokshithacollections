package com.ec.mokshitha_collections.service;

import org.springframework.stereotype.Service;
import java.util.List;

import com.ec.mokshitha_collections.dto.wishlist.WishlistItemResponse;
import com.ec.mokshitha_collections.entity.Product;
import com.ec.mokshitha_collections.entity.ProductVariant;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.entity.UserWishlist;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductVariantImageRepository;
import com.ec.mokshitha_collections.repository.ProductVariantRepository;
import com.ec.mokshitha_collections.repository.UserWishlistRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {
	private final UserWishlistRepository wishlistRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;

    /**
     * Toggles a specific variant in the user's wishlist. The same product can
     * be wishlisted in multiple colours — each variant is its own entry.
     * Returns true when added, false when removed.
     */
    @Transactional
    public boolean toggleWishlist(User user, Long variantId) {
        return wishlistRepository
            .findByUserUserIdAndVariantVariantId(user.getUserId(), variantId)
            .map(existing -> {
                wishlistRepository.delete(existing);
                return false; // removed
            })
            .orElseGet(() -> {
                ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

                wishlistRepository.save(
                    UserWishlist.builder()
                        .user(user)
                        .variant(variant)
                        .product(variant.getProduct()) // kept for display/grouping
                        .build()
                );
                return true; // added
            });
    }

    // Get wishlist items for a user, each with the saved variant's image
    // (primary/first image, falling back to the product hero — same as the cart).
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserUserIdWithVariant(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private WishlistItemResponse toResponse(UserWishlist w) {
        ProductVariant v = w.getVariant();
        Product p = w.getProduct();
        return WishlistItemResponse.builder()
                .wishlistId(w.getWishlistId())
                .productId(p.getProductId())
                .productName(p.getName())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .imageUrl(variantImageUrl(v, p))
                .variantId(v.getVariantId())
                .color(v.getColor())
                .size(v.getSize())
                .build();
    }

    /** Variant's primary/first image, falling back to the product hero. */
    private String variantImageUrl(ProductVariant v, Product p) {
        return variantImageRepository
                .findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(v.getVariantId())
                .stream()
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElseGet(p::getImageUrl);
    }
}
