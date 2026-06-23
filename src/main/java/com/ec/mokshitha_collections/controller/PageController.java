package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.CartService;
import com.ec.mokshitha_collections.service.CategoryService;
import com.ec.mokshitha_collections.service.OrderService;
import com.ec.mokshitha_collections.service.ProductService;
import com.ec.mokshitha_collections.service.ReviewService;
import com.ec.mokshitha_collections.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ec.mokshitha_collections.dto.cart.CartItemResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wires every Thymeleaf template under src/main/resources/templates to a URL.
 * `user`, `wishlistCount`, `cartCount`, `wishlistProductIds` are populated
 * automatically by GlobalModelAdvice when the visitor is authenticated.
 */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final CartService cartService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final OrderService orderService;
    private final WishlistService wishlistService;

    private static final int DEFAULT_PAGE_SIZE = 12;

    @GetMapping("/about")          public String about()         { return "about"; }
    @GetMapping("/contact")        public String contact()       { return "contact"; }
    @GetMapping("/login_register") public String loginRegister() { return "login_register"; }

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String categorySlug,
                       @RequestParam(required = false) BigDecimal minPrice,
                       @RequestParam(required = false) BigDecimal maxPrice,
                       @RequestParam(required = false) String search,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "createdAt,desc") String sort,
                       Model model) {

        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE, parseSort(sort));
        var products = productService.listProducts(
                categoryId, categorySlug, minPrice, maxPrice, search, null, pageable);

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.listActive());
        model.addAttribute("filters", new ShopFilters(categoryId, categorySlug, minPrice, maxPrice, search, sort));
        return "shop";
    }

    @GetMapping("/product-detail/{productId}")
    public String productDetail(@PathVariable Long productId,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {
        Long currentUserId = principal != null ? principal.getUserId() : null;

        model.addAttribute("product", productService.getProductDetail(productId));
        model.addAttribute("reviews",
                reviewService.listForProduct(productId, currentUserId,
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "product-detail";
    }

    @GetMapping("/cart")
    public String cart(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("cart", cartService.getCart(principal.getUserId()));
        return "cart";
    }

    /** Dedicated wishlist page (opened from the header heart icon). */
    @GetMapping("/wishlist")
    public String wishlist(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("wishlistItems", wishlistService.getUserWishlist(principal.getUserId()));
        return "wishlist";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam(required = false) Long variantId,
                           @RequestParam(required = false) Integer qty,
                           @AuthenticationPrincipal CustomUserDetails principal, Model model) {

        List<CartItemResponse> checkoutItems;
        int droppedCount = 0;
        boolean buyNow = variantId != null;

        if (buyNow) {
            // ----- Buy Now: a single selected variant; the cart is left untouched -----
            checkoutItems = List.of(orderService.buyNowItem(variantId, qty == null ? 1 : qty));
            model.addAttribute("buyNow", true);
            model.addAttribute("buyNowVariantId", variantId);
            model.addAttribute("buyNowQty", qty == null ? 1 : qty);
        } else {
            // ----- Cart checkout: every in-stock line (out-of-stock dropped here) -----
            var cart = cartService.getCart(principal.getUserId());
            checkoutItems = cart.getItems().stream()
                    .filter(i -> i.getStockAvailable() == null || i.getStockAvailable() > 0)
                    .toList();
            droppedCount = cart.getItems().size() - checkoutItems.size();
            model.addAttribute("cart", cart);
        }

        BigDecimal subtotal = checkoutItems.stream()
                .map(i -> i.getLineTotal() != null ? i.getLineTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = orderService.calculateShipping(subtotal);

        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("checkoutSubtotal", subtotal);
        model.addAttribute("droppedCount", droppedCount);
        model.addAttribute("shippingFee", shipping);
        model.addAttribute("orderTotal", subtotal.add(shipping));
        return "checkout";
    }

    /**
     * Post-checkout celebration page. Ownership is enforced by
     * orderService.getById which throws ResourceNotFoundException if the
     * order doesn't belong to the caller.
     */
    @GetMapping("/orders/{orderId}/success")
    public String orderSuccess(@PathVariable Long orderId,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {
        model.addAttribute("order", orderService.getById(principal.getUserId(), orderId));
        return "order-success";
    }

    /**
     * Customer order-tracking page: shows which fulfilment phase the order is
     * in, the courier tracking link and expected delivery date. Ownership is
     * enforced by orderService.getById (throws if the order isn't the caller's).
     */
    @GetMapping("/orders/{orderId}/track")
    public String orderTrack(@PathVariable Long orderId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             Model model) {
        var order = orderService.getById(principal.getUserId(), orderId);
        model.addAttribute("order", order);
        // Show each ordered variant's image rather than the product hero.
        model.addAttribute("variantImages", orderService.variantImagesForOrder(order));
        return "order-track";
    }

    private static Sort parseSort(String sort) {
        // "field,direction" — fall back to id desc if malformed.
        try {
            String[] parts = sort.split(",", 2);
            String field = parts[0].trim();
            Sort.Direction dir = parts.length > 1
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;
            return Sort.by(dir, field);
        } catch (Exception ignored) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    /** Holds the active shop filter selection so the template can reflect them in the UI. */
    public record ShopFilters(Long categoryId, String categorySlug, BigDecimal minPrice,
                              BigDecimal maxPrice, String search, String sort) {}
}
