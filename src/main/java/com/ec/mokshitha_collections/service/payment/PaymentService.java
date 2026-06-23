package com.ec.mokshitha_collections.service.payment;

import com.ec.mokshitha_collections.entity.*;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.OutOfStockException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.*;
import com.ec.mokshitha_collections.service.OfferService;
import com.ec.mokshitha_collections.service.OfferService.DiscountLine;
import com.ec.mokshitha_collections.service.OfferService.DiscountResult;
import com.ec.mokshitha_collections.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Razorpay "reserve-at-Pay" flow (Strategy A):
 *  - {@link #createPayment} reserves stock atomically, creates a Razorpay order,
 *    and stores a {@link PendingCheckout} snapshot. No real Order yet.
 *  - {@link #confirm} (called after browser/webhook signature checks) turns the
 *    snapshot into a paid Order, clears the cart, marks the hold CONFIRMED.
 *  - {@link #release} returns reserved stock on failure; {@link #releaseExpired}
 *    sweeps abandoned holds on a schedule.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserCartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final PendingCheckoutRepository pendingRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpay;
    private final OrderService orderService;
    private final OfferService offerService;
    private final OfferRepository offerRepository;
    private final OfferRedemptionRepository offerRedemptionRepository;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${app.checkout.hold-minutes:15}")
    private int holdMinutes;

    /** Cart checkout: reserve every cart line and open a Razorpay order. */
    @Transactional
    public Map<String, Object> createPayment(Long userId, Long addressId) {
        return createPayment(userId, addressId, null, null, null);
    }

    /**
     * Opens a Razorpay order after reserving stock. When {@code buyNowVariantId}
     * is provided this is a single-item "Buy Now" checkout (the cart is ignored
     * and untouched); otherwise it reserves the whole cart. An optional
     * {@code offerCode} is re-validated and applied server-side; the browser's
     * idea of the discount is never trusted.
     */
    @Transactional
    public Map<String, Object> createPayment(Long userId, Long addressId,
                                             Long buyNowVariantId, Integer buyNowQty,
                                             String offerCode) {
        boolean buyNow = buyNowVariantId != null;
        List<DiscountLine> discountLines = new ArrayList<>();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this address");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PendingCheckoutItem> snapshot = new ArrayList<>();

        if (buyNow) {
            // ----- Buy Now: a single selected variant -----
            int qty = (buyNowQty == null || buyNowQty < 1) ? 1 : buyNowQty;
            ProductVariant v = variantRepository.findById(buyNowVariantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant no longer exists"));
            int stock = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
            if (stock <= 0 || variantRepository.reserveStock(v.getVariantId(), qty) == 0) {
                throw new OutOfStockException(stock <= 0
                        ? v.getProduct().getName() + " is out of stock"
                        : "Only " + stock + " left of " + v.getProduct().getName()
                          + (v.getColor() != null ? " (" + v.getColor() + ")" : ""));
            }
            subtotal = addSnapshotItem(snapshot, v, qty);
            discountLines.add(OfferService.lineFor(v, qty));
        } else {
            // ----- Cart checkout: every in-stock line -----
            UserCart cart = cartRepository.findByUserIdWithItems(userId)
                    .orElseThrow(() -> new BadRequestException("Your cart is empty"));
            if (cart.getItems().isEmpty()) {
                throw new BadRequestException("Your cart is empty");
            }
            for (CartItem ci : cart.getItems()) {
                ProductVariant v = variantRepository.findById(ci.getVariant().getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant no longer exists"));
                int stock = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
                if (stock <= 0) continue; // out of stock — dropped, same as the checkout view

                int qty = ci.getQuantity();
                if (variantRepository.reserveStock(v.getVariantId(), qty) == 0) {
                    throw new OutOfStockException("Only " + stock + " left of "
                            + v.getProduct().getName()
                            + (v.getColor() != null ? " (" + v.getColor() + ")" : ""));
                }
                subtotal = subtotal.add(addSnapshotItem(snapshot, v, qty));
                discountLines.add(OfferService.lineFor(v, qty));
            }
            if (snapshot.isEmpty()) {
                throw new BadRequestException("All items in your cart are out of stock");
            }
        }

        BigDecimal shipping = orderService.calculateShipping(subtotal);

        // Re-validate + compute the coupon discount server-side (never trust the
        // browser's number). Item-scoped: only matching lines reduce the total.
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCode = null;
        if (offerCode != null && !offerCode.isBlank()) {
            DiscountResult dr = offerService.computeDiscount(userId, offerCode, discountLines);
            discount = dr.discountAmount();
            appliedCode = dr.offer().getCode();
        }

        BigDecimal total = subtotal.add(shipping).subtract(discount);
        if (total.signum() < 0) total = BigDecimal.ZERO; // safety: never charge negative
        long amountPaise = total.multiply(BigDecimal.valueOf(100)).longValueExact();

        String receipt = "rcpt_" + userId + "_" + (System.currentTimeMillis() % 100000000L);
        // Razorpay order is created AFTER the reserves; if it fails the whole
        // transaction rolls back and the reserved stock is restored.
        String razorpayOrderId = razorpay.createOrder(amountPaise, currency, receipt);

        PendingCheckout pending = PendingCheckout.builder()
                .userId(userId)
                .razorpayOrderId(razorpayOrderId)
                .subtotal(subtotal)
                .shippingFee(shipping)
                .totalAmount(total)
                .shippingAddress(AddressSnapshot.from(address))
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PendingCheckoutStatus.HELD)
                .buyNow(buyNow)
                .offerCode(appliedCode)
                .discountAmount(discount.signum() > 0 ? discount : null)
                .expiresAt(LocalDateTime.now().plusMinutes(holdMinutes))
                .build();
        snapshot.forEach(it -> { it.setPendingCheckout(pending); pending.getItems().add(it); });
        pendingRepository.save(pending);

        return Map.of(
                "razorpayOrderId", razorpayOrderId,
                "razorpayKeyId", razorpay.getKeyId(),
                "amount", amountPaise,
                "currency", currency,
                "customerName", safe(user.getFirstName()) + " " + safe(user.getLastName()),
                "customerEmail", safe(user.getEmail()),
                "customerContact", safe(user.getPhone()));
    }

    /**
     * Finalises a paid checkout into a real Order. Idempotent: a repeat call for
     * an already-confirmed checkout returns the existing order id.
     */
    @Transactional
    public Long confirm(String razorpayOrderId, String razorpayPaymentId) {
        PendingCheckout pending = pendingRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));

        if (pending.getStatus() == PendingCheckoutStatus.CONFIRMED) {
            // Already processed (e.g. webhook + browser both fired) — return the order.
            return orderRepository.findByRazorpayOrderId(razorpayOrderId)
                    .map(Order::getOrderId).orElse(null);
        }
        if (pending.getStatus() == PendingCheckoutStatus.RELEASED) {
            // Hold expired/failed and stock was returned — can't fulfil now.
            throw new BadRequestException("This checkout has expired. Please try again.");
        }

        Order order = Order.builder()
                .user(userRepository.getReferenceById(pending.getUserId()))
                .shippingAddress(pending.getShippingAddress())
                .subtotal(pending.getSubtotal())
                .shippingFee(pending.getShippingFee())
                .offerCode(pending.getOfferCode())
                .discountAmount(pending.getDiscountAmount())
                .totalAmount(pending.getTotalAmount())
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentStatus(PaymentStatus.PAID)
                .status(OrderStatus.PLACED)
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(razorpayPaymentId)
                .build();
        Set<Long> orderedVariantIds = new HashSet<>();
        for (PendingCheckoutItem pi : pending.getItems()) {
            OrderItem oi = OrderItem.builder()
                    .productId(pi.getProductId())
                    .productName(pi.getProductName())
                    .productImageUrl(pi.getProductImageUrl())
                    .variantId(pi.getVariantId())
                    .variantColor(pi.getVariantColor())
                    .variantSize(pi.getVariantSize())
                    .skuVariant(pi.getSkuVariant())
                    .unitPrice(pi.getUnitPrice())
                    .quantity(pi.getQuantity())
                    .lineTotal(pi.getLineTotal())
                    .build();
            oi.setOrder(order);
            order.getItems().add(oi);
            orderedVariantIds.add(pi.getVariantId());
        }
        Order saved = orderRepository.save(order);

        // Record the coupon usage (per-customer-limit + audit). Look the offer up
        // by code; if it was deleted in the meantime, skip the record but keep the
        // discount on the order.
        if (pending.getOfferCode() != null) {
            offerRepository.findByCodeIgnoreCase(pending.getOfferCode()).ifPresent(offer ->
                offerRedemptionRepository.save(OfferRedemption.builder()
                        .offerId(offer.getOfferId())
                        .userId(pending.getUserId())
                        .orderId(saved.getOrderId())
                        .code(pending.getOfferCode())
                        .discountAmount(pending.getDiscountAmount())
                        .build()));
        }

        // Remove the purchased lines from the cart (leave anything else) — but a
        // "Buy Now" checkout never used the cart, so leave it completely alone.
        if (!Boolean.TRUE.equals(pending.getBuyNow())) {
            cartRepository.findByUserIdWithItems(pending.getUserId()).ifPresent(cart -> {
                cart.getItems().removeIf(ci -> orderedVariantIds.contains(ci.getVariant().getVariantId()));
                cartRepository.save(cart);
            });
        }

        pending.setStatus(PendingCheckoutStatus.CONFIRMED);
        pendingRepository.save(pending);
        return saved.getOrderId();
    }

    /** Returns reserved stock and marks the hold RELEASED (failure / expiry). */
    @Transactional
    public void release(String razorpayOrderId) {
        pendingRepository.findByRazorpayOrderId(razorpayOrderId)
                .filter(p -> p.getStatus() == PendingCheckoutStatus.HELD)
                .ifPresent(this::releasePending);
    }

    private void releasePending(PendingCheckout pending) {
        for (PendingCheckoutItem it : pending.getItems()) {
            variantRepository.releaseStock(it.getVariantId(), it.getQuantity());
        }
        pending.setStatus(PendingCheckoutStatus.RELEASED);
        pendingRepository.save(pending);
        log.info("Released reservation for Razorpay order {}", pending.getRazorpayOrderId());
    }

    /** Sweeps abandoned holds (browser closed, never paid) every minute. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpired() {
        List<PendingCheckout> expired = pendingRepository
                .findByStatusAndExpiresAtBefore(PendingCheckoutStatus.HELD, LocalDateTime.now());
        for (PendingCheckout p : expired) {
            releasePending(p);
        }
    }

    /** Appends a snapshot line for a (already-reserved) variant; returns its line total. */
    private static BigDecimal addSnapshotItem(List<PendingCheckoutItem> snapshot, ProductVariant v, int qty) {
        Product p = v.getProduct();
        BigDecimal unitPrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        snapshot.add(PendingCheckoutItem.builder()
                .productId(p.getProductId())
                .productName(p.getName())
                .productImageUrl(p.getImageUrl())
                .variantId(v.getVariantId())
                .variantColor(v.getColor())
                .variantSize(v.getSize())
                .skuVariant(v.getSkuVariant())
                .unitPrice(unitPrice)
                .quantity(qty)
                .lineTotal(lineTotal)
                .build());
        return lineTotal;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
