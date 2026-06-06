package com.ec.mokshitha_collections.config;

import com.ec.mokshitha_collections.controller.HomeController;
import com.ec.mokshitha_collections.controller.PageController;
import com.ec.mokshitha_collections.controller.UserController;
import com.ec.mokshitha_collections.controller.admin.AdminPageController;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.repository.CartItemRepository;
import com.ec.mokshitha_collections.repository.UserCartRepository;
import com.ec.mokshitha_collections.repository.UserRepository;
import com.ec.mokshitha_collections.repository.UserWishlistRepository;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Populates `user`, `wishlistCount`, `wishlistProductIds`, `cartCount` on
 * every Thymeleaf view render so the header badges and account widgets have
 * data without each handler having to fetch it.
 *
 * Limited to view-rendering controllers via basePackages so it doesn't fire
 * on REST endpoints (which return JSON and don't need view models).
 */
@ControllerAdvice(assignableTypes = {
        HomeController.class,
        UserController.class,
        PageController.class,
        AdminPageController.class
        // Add new view-rendering controllers here so their templates get
        // `user`, `wishlistCount`, `cartCount`, `wishlistProductIds`.
})
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserRepository userRepository;
    private final UserWishlistRepository userWishlistRepository;
    private final UserCartRepository userCartRepository;
    private final CartItemRepository cartItemRepository;

    @ModelAttribute
    @Transactional(readOnly = true)
    public void populate(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (principal == null) return;

        Long userId = principal.getUserId();

        User user = userRepository.findByIdWithAddresses(userId).orElse(null);
        if (user == null) return;

        long wishlistCount = userWishlistRepository.countByUserUserId(userId);
        long cartCount = userCartRepository.findByUserUserId(userId)
                .map(c -> cartItemRepository.countByCartCartId(c.getCartId()))
                .orElse(0L);

        model.addAttribute("user", user);
        model.addAttribute("wishlistCount", wishlistCount);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("wishlistProductIds",
                userWishlistRepository.findProductIdsByUserId(userId));
        // Variant ids in the wishlist — product-detail uses this for its
        // per-variant heart, and the card pickers mark "already added" variants.
        model.addAttribute("wishlistVariantIds",
                userWishlistRepository.findVariantIdsByUserId(userId));
        // Distinct product ids currently in the cart — lets product cards
        // render "View Cart" instead of "Add to Cart" on first paint.
        model.addAttribute("cartProductIds",
                cartItemRepository.findProductIdsByUserId(userId));
        // Variant ids in the cart — the product-detail page uses this to flip
        // its button per selected variant (a product can have some variants in
        // the cart and others not).
        model.addAttribute("cartVariantIds",
                cartItemRepository.findVariantIdsByUserId(userId));
    }
}
