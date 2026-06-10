package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.order.AddressSnapshotResponse;
import com.ec.mokshitha_collections.dto.order.OrderItemResponse;
import com.ec.mokshitha_collections.dto.order.OrderResponse;
import com.ec.mokshitha_collections.dto.order.OrderSummaryResponse;
import com.ec.mokshitha_collections.dto.order.PlaceOrderRequest;
import com.ec.mokshitha_collections.entity.*;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.OutOfStockException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.AddressRepository;
import com.ec.mokshitha_collections.repository.OrderRepository;
import com.ec.mokshitha_collections.repository.ProductVariantImageRepository;
import com.ec.mokshitha_collections.repository.ProductVariantRepository;
import com.ec.mokshitha_collections.repository.UserCartRepository;
import com.ec.mokshitha_collections.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserCartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;

    /** Flat delivery charge added when the subtotal is below the free threshold. */
    @Value("${app.shipping.flat-fee:100.00}")
    private BigDecimal flatShippingFee;

    /** Orders with a subtotal at or above this amount ship free. */
    @Value("${app.shipping.free-above:5000.00}") //app.shipping.free-above:5000.00
    private BigDecimal freeShippingThreshold;

    /**
     * Delivery charge for a given subtotal: the flat fee when below the free
     * threshold, otherwise free. Single source of truth so the checkout page
     * preview and the placed order always agree.
     */
    public BigDecimal calculateShipping(BigDecimal subtotal) {
        if (subtotal == null) return flatShippingFee;
        return subtotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : flatShippingFee;
    }

    /**
     * Atomically converts the user's cart into an Order:
     *  - re-validates stock for every line (cart could have been added to days ago)
     *  - decrements variant stock
     *  - snapshots product/price/address into immutable order/order-item rows
     *  - empties the cart
     * Any failure (out of stock, missing address, etc.) rolls everything back.
     */
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        UserCart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this address");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        // Cart lines we actually ordered — removed from the cart afterwards.
        // Out-of-stock lines are left in place so the customer can buy them later.
        List<CartItem> orderedLines = new ArrayList<>();

        for (CartItem ci : cart.getItems()) {
            ProductVariant v = ci.getVariant();
            // Re-fetch with lock-free read; we'll save below to flush stock change.
            ProductVariant fresh = variantRepository.findById(v.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant " + v.getVariantId() + " no longer exists"));

            int qty = ci.getQuantity();
            int stock = fresh.getStockQuantity() == null ? 0 : fresh.getStockQuantity();

            // Out of stock → silently skip; it stays in the cart, not in the order.
            if (stock <= 0) {
                continue;
            }
            // In stock but not enough for the requested quantity → genuine error.
            if (stock < qty) {
                throw new OutOfStockException("Only " + stock + " left of "
                        + fresh.getProduct().getName()
                        + (fresh.getColor() != null ? " (" + fresh.getColor() + ")" : ""));
            }

            // Decrement stock — committed atomically with the order insert.
            fresh.setStockQuantity(stock - qty);
            variantRepository.save(fresh);

            Product p = fresh.getProduct();
            BigDecimal unitPrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(lineTotal);

            orderItems.add(OrderItem.builder()
                    .productId(p.getProductId())
                    .productName(p.getName())
                    .productImageUrl(p.getImageUrl())
                    .variantId(fresh.getVariantId())
                    .variantColor(fresh.getColor())
                    .variantSize(fresh.getSize())
                    .skuVariant(fresh.getSkuVariant())
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .lineTotal(lineTotal)
                    .build());
            orderedLines.add(ci);
        }

        if (orderItems.isEmpty()) {
            throw new BadRequestException("All items in your cart are out of stock");
        }

        BigDecimal shipping = calculateShipping(subtotal);
        BigDecimal total = subtotal.add(shipping);

        Order order = Order.builder()
                .user(user)
                .shippingAddress(AddressSnapshot.from(address))
                .subtotal(subtotal)
                .shippingFee(shipping)
                .totalAmount(total)
                .paymentMethod(req.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING) // COD: paid on delivery
                .status(OrderStatus.PLACED)
                .build();
        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            order.getItems().add(oi);
        }
        Order saved = orderRepository.save(order);

        // Remove only the ordered (in-stock) lines; any out-of-stock lines stay
        // in the cart. orphanRemoval=true on UserCart.items handles deletion.
        cart.getItems().removeAll(orderedLines);
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listForUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserUserIdOrderByPlacedAtDesc(userId, pageable)
                .map(OrderService::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    /**
     * variantId -> the variant's primary/first image, so order views can show
     * the ordered variant's image instead of the product hero. Resolved live
     * from the variant id stored on each order line; variants with no image of
     * their own are simply absent (the view falls back to the snapshot hero).
     */
    @Transactional(readOnly = true)
    public Map<Long, String> variantImagesForOrder(OrderResponse order) {
        Map<Long, String> images = new HashMap<>();
        if (order == null || order.getItems() == null) return images;
        order.getItems().forEach(item -> {
            Long variantId = item.getVariantId();
            if (variantId == null || images.containsKey(variantId)) return;
            variantImageRepository
                    .findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(variantId)
                    .stream().findFirst()
                    .ifPresent(img -> images.put(variantId, img.getImageUrl()));
        });
        return images;
    }

    /**
     * Cancels an order if it's still in a cancellable state and re-stocks each line.
     * Variants that have since been deleted are skipped (best-effort restock).
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getStatus().isCancellable()) {
            throw new BadRequestException("This order can no longer be cancelled");
        }

        for (OrderItem oi : order.getItems()) {
            variantRepository.findById(oi.getVariantId()).ifPresent(v -> {
                int current = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
                v.setStockQuantity(current + oi.getQuantity());
                variantRepository.save(v);
            });
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        return toResponse(orderRepository.save(order));
    }

    /* ---------- Mappers ---------- */

    public static OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .razorpayOrderId(o.getRazorpayOrderId())
                .razorpayPaymentId(o.getRazorpayPaymentId())
                .subtotal(o.getSubtotal())
                .shippingFee(o.getShippingFee())
                .totalAmount(o.getTotalAmount())
                .placedAt(o.getPlacedAt())
                .updatedAt(o.getUpdatedAt())
                .confirmedAt(o.getConfirmedAt())
                .processingAt(o.getProcessingAt())
                .shippedAt(o.getShippedAt())
                .deliveredAt(o.getDeliveredAt())
                .cancelledAt(o.getCancelledAt())
                .trackingNumber(o.getTrackingNumber())
                .courier(o.getCourier())
                .trackingUrl(o.getTrackingUrl())
                .expectedDeliveryDate(o.getExpectedDeliveryDate())
                .shippingAddress(toAddressResponse(o.getShippingAddress()))
                .items(o.getItems().stream().map(OrderService::toItemResponse).toList())
                .build();
    }

    public static OrderSummaryResponse toSummary(Order o) {
        return OrderSummaryResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .paymentStatus(o.getPaymentStatus())
                .totalAmount(o.getTotalAmount())
                .itemCount(o.getItems().size())
                .placedAt(o.getPlacedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem oi) {
        return OrderItemResponse.builder()
                .orderItemId(oi.getOrderItemId())
                .productId(oi.getProductId())
                .productName(oi.getProductName())
                .productImageUrl(oi.getProductImageUrl())
                .variantId(oi.getVariantId())
                .variantColor(oi.getVariantColor())
                .variantSize(oi.getVariantSize())
                .unitPrice(oi.getUnitPrice())
                .quantity(oi.getQuantity())
                .lineTotal(oi.getLineTotal())
                .build();
    }

    private static AddressSnapshotResponse toAddressResponse(AddressSnapshot a) {
        if (a == null) return null;
        return AddressSnapshotResponse.builder()
                .firstName(a.getFirstName())
                .lastName(a.getLastName())
                .phone(a.getPhone())
                .streetAddress(a.getStreetAddress())
                .city(a.getCity())
                .state(a.getState())
                .pinCode(a.getPinCode())
                .country(a.getCountry())
                .addressType(a.getAddressType())
                .build();
    }
}
