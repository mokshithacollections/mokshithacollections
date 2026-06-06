package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.product.ProductDetailResponse;
import com.ec.mokshitha_collections.entity.OrderStatus;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.OrderRepository;
import com.ec.mokshitha_collections.repository.ProductRepository;
import com.ec.mokshitha_collections.repository.ProductReviewRepository;
import com.ec.mokshitha_collections.repository.UserRepository;
import com.ec.mokshitha_collections.repository.UserWishlistRepository;
import com.ec.mokshitha_collections.service.ProductService;
import com.ec.mokshitha_collections.service.admin.AdminCategoryService;
import com.ec.mokshitha_collections.service.admin.AdminOrderService;
import com.ec.mokshitha_collections.service.admin.AdminProductService;
import com.ec.mokshitha_collections.service.admin.AdminReviewService;
import com.ec.mokshitha_collections.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Server-rendered admin UI. All write operations are performed by the
 * existing /api/admin/** REST endpoints (called from per-page JS) — these
 * GET handlers only render the pages with the data they need.
 *
 * Gated by SecurityConfig: /admin/** requires hasRole("ADMIN").
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminProductService productService;
    private final AdminCategoryService categoryService;
    private final AdminOrderService orderService;
    private final AdminReviewService reviewService;
    private final AdminUserService userService;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductReviewRepository reviewRepository;
    private final UserWishlistRepository userWishlistRepository;

    private final ProductService customerProductService;

    /* ---------- Dashboard ---------- */

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("productCount", productRepository.count());
        model.addAttribute("orderCount", orderRepository.count());
        model.addAttribute("placedOrderCount", orderRepository.countByStatus(OrderStatus.PLACED));
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("pendingReviewCount",
                reviewRepository.findPending(PageRequest.of(0, 1)).getTotalElements());
        // Recent orders + recent products on the dashboard
        model.addAttribute("recentOrders",
                orderService.list(null, PageRequest.of(0, 5)).getContent());
        return "admin/dashboard";
    }

    /* ---------- Products ---------- */

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page,
                           Model model) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("products", productService.listAll(pageable));
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String productNew(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("product", null); // signals "new"
        return "admin/product-form";
    }

    @GetMapping("/products/{id}")
    public String productEdit(@PathVariable Long id, Model model) {
        ProductDetailResponse product = customerProductService.getProductDetailForAdmin(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.listAll());
        return "admin/product-form";
    }

    /* ---------- Categories ---------- */

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String categoryNew(Model model) {
        model.addAttribute("category", null);
        model.addAttribute("allCategories", categoryService.listAll());
        return "admin/category-form";
    }

    @GetMapping("/categories/{id}")
    public String categoryEdit(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.listAll().stream()
                .filter(c -> c.getCategoryId().equals(id))
                .findFirst().orElse(null));
        model.addAttribute("allCategories", categoryService.listAll());
        return "admin/category-form";
    }

    /* ---------- Orders ---------- */

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Pageable pageable = PageRequest.of(page, 20);
        model.addAttribute("orders", orderService.list(status, pageable));
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        var order = orderService.getById(id);
        model.addAttribute("order", order);
        // variantId -> the variant's first/primary image, so each line shows the
        // ordered variant's image rather than the product hero.
        model.addAttribute("variantImages", orderService.variantImagesForOrder(order));
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/order-detail";
    }

    /**
     * Print-friendly packing slip for the parcel. Designed for A5/A4 paper —
     * `window.print()` from the page strips the navigation and prints just
     * the slip.
     */
    @GetMapping("/orders/{id}/packing-slip")
    public String packingSlip(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        return "admin/packing-slip";
    }

    /* ---------- Reviews ---------- */

    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, 20);
        model.addAttribute("reviews", reviewService.listPending(pageable));
        return "admin/reviews";
    }

    /* ---------- Users ---------- */

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("users", userService.list(search, pageable));
        model.addAttribute("search", search);
        return "admin/users";
    }

    /**
     * Drill-down view for a single user: profile, stats, addresses, paged
     * orders, with the activate/promote actions inline so the admin can do
     * everything from one screen.
     */
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable("id") Long userId,
                             @RequestParam(name = "ordersPage", defaultValue = "0") int ordersPage,
                             Model model) {
        User userRow = userRepository.findByIdWithAddresses(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Go through the service so the transaction stays open long enough
        // for OrderService.toSummary's lazy `order.items` access to succeed.
        Pageable pageable = PageRequest.of(ordersPage, 10);
        var orders = orderService.listOrdersForUser(userId, pageable);

        model.addAttribute("u", userRow);
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", orderRepository.countByUserUserId(userId));
        model.addAttribute("totalSpent",  orderRepository.sumTotalSpentByUserIdOrZero(userId));
        model.addAttribute("wishlistCount", userWishlistRepository.countByUserUserId(userId));
        return "admin/user-detail";
    }
}
