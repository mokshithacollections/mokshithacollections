package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.OfferService;
import com.ec.mokshitha_collections.service.OfferService.DiscountLine;
import com.ec.mokshitha_collections.service.OfferService.DiscountResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Checkout coupon endpoint. Validates a promo code against the current checkout
 * context (cart, or a single Buy-Now variant) and returns the rupee discount to
 * preview. The amount is re-validated server-side at payment time, so this is a
 * preview only — never a source of truth for the charge.
 */
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestParam String code,
                                                     @RequestParam(required = false) Long variantId,
                                                     @RequestParam(required = false) Integer qty,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUserId();
        List<DiscountLine> lines = (variantId != null)
                ? offerService.linesForBuyNow(variantId, qty)
                : offerService.linesForCart(userId);

        DiscountResult result = offerService.computeDiscount(userId, code, lines);
        BigDecimal discount = result.discountAmount();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "code", result.offer().getCode(),
                "name", result.offer().getName(),
                "discountAmount", discount));
    }
}
