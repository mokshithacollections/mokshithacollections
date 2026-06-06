package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.OrderService;
import com.ec.mokshitha_collections.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final WishlistService wishlistService;
    private final OrderService orderService;

    @GetMapping("/user_redirect")
    public String userRedirect(@RequestParam(value = "redirect", required = false) String redirect,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               HttpSession session,
                               Model model) {

        if (principal == null) {
            if (redirect != null) {
                session.setAttribute("LOGIN_REDIRECT_URL", redirect);
            }
            return "login_register";
        }

        Long userId = principal.getUserId();

        // `user`, `wishlistCount`, `cartCount`, `wishlistProductIds` are added by
        // GlobalModelAdvice. The account page additionally needs the actual wishlist
        // line items + recent order history for the rendered tabs.
        model.addAttribute("wishlistItems", wishlistService.getUserWishlist(userId));
        model.addAttribute("recentOrders",
                orderService.listForUser(userId, PageRequest.of(0, 10)).getContent());

        return "account";
    }
}
