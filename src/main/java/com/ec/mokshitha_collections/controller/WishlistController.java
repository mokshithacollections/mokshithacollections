package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.UserRepository;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    /**
     * Toggles a specific variant in the wishlist. The path id is a VARIANT id
     * now (the wishlist is variant-level), so the same product can be saved in
     * multiple colours.
     */
    @PostMapping("/toggle/{variantId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @PathVariable Long variantId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // Spring Security already enforces auth on /wishlist/**; principal is non-null here.
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean added = wishlistService.toggleWishlist(user, variantId);
        return ResponseEntity.ok(Map.of("added", added, "variantId", variantId));
    }
}
