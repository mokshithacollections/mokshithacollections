package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.offer.OfferBannerResponse;
import com.ec.mokshitha_collections.entity.Offer;
import com.ec.mokshitha_collections.entity.OfferDiscountType;
import com.ec.mokshitha_collections.entity.Product;
import com.ec.mokshitha_collections.entity.ProductCategory;
import com.ec.mokshitha_collections.entity.ProductVariant;
import com.ec.mokshitha_collections.entity.UserCart;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.OfferRedemptionRepository;
import com.ec.mokshitha_collections.repository.OfferRepository;
import com.ec.mokshitha_collections.repository.ProductVariantRepository;
import com.ec.mokshitha_collections.repository.UserCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Customer-facing offer logic: the home-banner feed and the checkout coupon
 * validation/computation. Discounts are <b>item-scoped</b> — when an offer
 * restricts to products/categories, only the matching cart lines contribute
 * to the discountable subtotal (empty restriction = the whole order).
 *
 * All amounts are recomputed server-side; the browser-supplied total is never
 * trusted (the same {@link #computeDiscount} is what the payment flow uses).
 */
@Service
@RequiredArgsConstructor
public class OfferService {

    private static final DateTimeFormatter HUMAN_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final OfferRepository offerRepository;
    private final OfferRedemptionRepository redemptionRepository;
    private final ProductVariantRepository variantRepository;
    private final UserCartRepository cartRepository;

    /* ===================== Home banner ===================== */

    /** LIVE + SCHEDULED offers for the home-page banner (empty = hide the banner). */
    @Transactional(readOnly = true)
    public List<OfferBannerResponse> listActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return offerRepository.findByActiveTrueAndEndAtAfterOrderByPriorityDescStartAtAsc(now)
                .stream().map(o -> toBanner(o, now)).toList();
    }

    private static OfferBannerResponse toBanner(Offer o, LocalDateTime now) {
        boolean scheduled = now.isBefore(o.getStartAt());
        LocalDateTime target = scheduled ? o.getStartAt() : o.getEndAt();
        long secs = Math.max(0, Duration.between(now, target).getSeconds());

        String label;
        if (o.getDiscountType() == OfferDiscountType.PERCENTAGE) {
            label = strip(o.getDiscountValue()) + "% OFF";
        } else {
            label = "₹" + strip(o.getDiscountValue()) + " OFF";
        }

        // "23 Jun → 30 Jun 2026" (drop the redundant year on the start when both share it).
        boolean sameYear = o.getStartAt().getYear() == o.getEndAt().getYear();
        String startStr = (sameYear ? DAY_MONTH : DAY_MONTH_YEAR).format(o.getStartAt());
        String validity = startStr + " → " + DAY_MONTH_YEAR.format(o.getEndAt());

        return OfferBannerResponse.builder()
                .offerId(o.getOfferId())
                .name(o.getName())
                .code(o.getCode())
                .description(o.getDescription())
                .discountLabel(label)
                .mode(scheduled ? "SCHEDULED" : "LIVE")
                .countdownLabel(scheduled ? "Starts in" : "Ends in")
                .secondsRemaining(secs)
                .validityText(validity)
                .build();
    }

    /* ===================== Checkout coupon ===================== */

    /** One discountable checkout line — the minimum an offer needs to score it. */
    public record DiscountLine(Long productId, Long categoryId, Long parentCategoryId, BigDecimal lineTotal) {}

    /** Outcome of a valid coupon: the matched offer + the rupee discount to apply. */
    public record DiscountResult(Offer offer, BigDecimal discountAmount) {}

    /**
     * Validates {@code code} against the given checkout lines for {@code userId}
     * and returns the discount to apply. Throws {@link BadRequestException} with
     * a customer-friendly message when the code can't be used.
     */
    @Transactional(readOnly = true)
    public DiscountResult computeDiscount(Long userId, String code, List<DiscountLine> lines) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Enter a promo code.");
        }
        Offer offer = offerRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("That promo code isn't valid."));
        return computeForOffer(offer, userId, lines);
    }

    /**
     * Best auto-apply offer for the given lines/user, or empty if none is
     * eligible. Ranked by priority (desc), then by the larger actual discount.
     * Only one offer is ever applied — the system never stacks coupons.
     */
    @Transactional(readOnly = true)
    public Optional<DiscountResult> bestAutoApply(Long userId, List<DiscountLine> lines) {
        DiscountResult best = null;
        for (Offer o : offerRepository.findByActiveTrueAndAutoApplyTrue()) {
            DiscountResult r;
            try {
                r = computeForOffer(o, userId, lines);   // reuses all eligibility checks
            } catch (BadRequestException notEligible) {
                continue;
            }
            if (best == null) { best = r; continue; }
            int byPriority = Integer.compare(o.getPriority(), best.offer().getPriority());
            if (byPriority > 0 || (byPriority == 0 && r.discountAmount().compareTo(best.discountAmount()) > 0)) {
                best = r;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Full eligibility + discount computation for one offer (throws on any failure). */
    private DiscountResult computeForOffer(Offer offer, Long userId, List<DiscountLine> lines) {
        LocalDateTime now = LocalDateTime.now();
        if (offer.getActive() == null || !offer.getActive()) {
            throw new BadRequestException("This promo code isn't active right now.");
        }
        if (now.isBefore(offer.getStartAt())) {
            throw new BadRequestException("This offer starts on " + offer.getStartAt().format(HUMAN_DATE) + ".");
        }
        if (now.isAfter(offer.getEndAt())) {
            throw new BadRequestException("This promo code has expired.");
        }

        BigDecimal fullSubtotal = lines.stream()
                .map(DiscountLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (offer.getMinOrderAmount() != null && fullSubtotal.compareTo(offer.getMinOrderAmount()) < 0) {
            BigDecimal more = offer.getMinOrderAmount().subtract(fullSubtotal);
            throw new BadRequestException("Add ₹" + strip(more) + " more to use this code (minimum order ₹"
                    + strip(offer.getMinOrderAmount()) + ").");
        }

        if (offer.getPerCustomerLimit() != null
                && redemptionRepository.countByOfferIdAndUserId(offer.getOfferId(), userId) >= offer.getPerCustomerLimit()) {
            throw new BadRequestException("You've already used this code the maximum number of times.");
        }

        BigDecimal applicable = lines.stream()
                .filter(l -> applies(offer, l))
                .map(DiscountLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (applicable.signum() <= 0) {
            throw new BadRequestException("This code doesn't apply to the items in your order.");
        }

        BigDecimal discount;
        if (offer.getDiscountType() == OfferDiscountType.PERCENTAGE) {
            discount = applicable.multiply(offer.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = offer.getDiscountValue().min(applicable);
        }
        if (offer.getMaxDiscountAmount() != null) {
            discount = discount.min(offer.getMaxDiscountAmount());
        }
        discount = discount.min(fullSubtotal).setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() <= 0) {
            throw new BadRequestException("This code gives no discount on your order.");
        }
        return new DiscountResult(offer, discount);
    }

    /** Item-scoped match: empty restriction = whole order; else product/category/parent-category. */
    private static boolean applies(Offer offer, DiscountLine line) {
        boolean noProducts = offer.getProductIds() == null || offer.getProductIds().isEmpty();
        boolean noCategories = offer.getCategoryIds() == null || offer.getCategoryIds().isEmpty();
        if (noProducts && noCategories) return true;
        if (!noProducts && offer.getProductIds().contains(line.productId())) return true;
        if (!noCategories) {
            if (line.categoryId() != null && offer.getCategoryIds().contains(line.categoryId())) return true;
            if (line.parentCategoryId() != null && offer.getCategoryIds().contains(line.parentCategoryId())) return true;
        }
        return false;
    }

    /* ----- Line builders so the preview endpoint and PaymentService agree ----- */

    /** Build discount lines for the user's in-stock cart (out-of-stock lines dropped). */
    @Transactional(readOnly = true)
    public List<DiscountLine> linesForCart(Long userId) {
        UserCart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        List<DiscountLine> lines = new ArrayList<>();
        cart.getItems().forEach(ci -> {
            ProductVariant v = ci.getVariant();
            int stock = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
            if (stock <= 0) return;
            lines.add(lineFor(v, ci.getQuantity()));
        });
        if (lines.isEmpty()) throw new BadRequestException("Your cart has no in-stock items");
        return lines;
    }

    /** Build the single discount line for a "Buy Now" checkout. */
    @Transactional(readOnly = true)
    public List<DiscountLine> linesForBuyNow(Long variantId, Integer qty) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant no longer exists"));
        int q = (qty == null || qty < 1) ? 1 : qty;
        return List.of(lineFor(v, q));
    }

    /** Build a single line from a variant + quantity (price logic mirrors PaymentService). */
    public static DiscountLine lineFor(ProductVariant v, int qty) {
        Product p = v.getProduct();
        BigDecimal unitPrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        ProductCategory cat = p.getCategory();
        Long catId = cat != null ? cat.getCategoryId() : null;
        Long parentId = (cat != null && cat.getParent() != null) ? cat.getParent().getCategoryId() : null;
        return new DiscountLine(p.getProductId(), catId, parentId, lineTotal);
    }

    private static String strip(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
