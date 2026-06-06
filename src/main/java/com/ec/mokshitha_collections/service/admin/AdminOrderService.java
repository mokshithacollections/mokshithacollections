package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.dto.admin.OrderStatusUpdateRequest;
import com.ec.mokshitha_collections.dto.order.OrderItemResponse;
import com.ec.mokshitha_collections.dto.order.OrderResponse;
import com.ec.mokshitha_collections.dto.order.OrderSummaryResponse;
import com.ec.mokshitha_collections.entity.Order;
import com.ec.mokshitha_collections.entity.OrderItem;
import com.ec.mokshitha_collections.entity.OrderStatus;
import com.ec.mokshitha_collections.entity.PaymentStatus;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.OrderRepository;
import com.ec.mokshitha_collections.repository.ProductVariantImageRepository;
import com.ec.mokshitha_collections.repository.ProductVariantRepository;
import com.ec.mokshitha_collections.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> list(OrderStatus statusFilter, Pageable pageable) {
        Page<Order> page = (statusFilter == null)
                ? orderRepository.findAllByOrderByPlacedAtDesc(pageable)
                : orderRepository.findByStatusOrderByPlacedAtDesc(statusFilter, pageable);
        return page.map(OrderService::toSummary);
    }

    /**
     * Orders placed by a specific user. Runs inside a read-only transaction
     * so OrderService.toSummary can safely call order.getItems().size() on
     * the lazy collection.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listOrdersForUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserUserIdOrderByPlacedAtDesc(userId, pageable)
                .map(OrderService::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return OrderService.toResponse(order);
    }

    /**
     * For each line in the order, the variant's primary/first image (resolved
     * live from the variant id), keyed by variantId. The admin order view uses
     * this to show the actual variant image instead of the product hero. A
     * variant with no images of its own is simply absent (caller falls back).
     */
    @Transactional(readOnly = true)
    public Map<Long, String> variantImagesForOrder(OrderResponse order) {
        Map<Long, String> images = new HashMap<>();
        if (order == null || order.getItems() == null) return images;
        for (OrderItemResponse item : order.getItems()) {
            Long variantId = item.getVariantId();
            if (variantId == null || images.containsKey(variantId)) continue;
            variantImageRepository
                    .findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(variantId)
                    .stream().findFirst()
                    .ifPresent(img -> images.put(variantId, img.getImageUrl()));
        }
        return images;
    }

    /**
     * Admin status transition. Stamps the per-status timestamp, optionally
     * applies tracking info (set when SHIPPED), and on CANCELLED re-stocks
     * each line + flips PAID payments to REFUNDED.
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatusUpdateRequest req) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus previous = order.getStatus();
        OrderStatus next = req.getStatus();

        // Re-stock + refund on cancel
        if (next == OrderStatus.CANCELLED && previous != OrderStatus.CANCELLED) {
            for (OrderItem oi : order.getItems()) {
                variantRepository.findById(oi.getVariantId()).ifPresent(v -> {
                    int current = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
                    v.setStockQuantity(current + oi.getQuantity());
                    variantRepository.save(v);
                });
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }

        // Stamp the timestamp for the new status the first time we enter it.
        // Re-applying the same status doesn't overwrite an earlier timestamp.
        if (previous != next) {
            LocalDateTime now = LocalDateTime.now();
            switch (next) {
                case CONFIRMED  -> { if (order.getConfirmedAt()  == null) order.setConfirmedAt(now);  }
                case PROCESSING -> { if (order.getProcessingAt() == null) order.setProcessingAt(now); }
                case SHIPPED    -> { if (order.getShippedAt()    == null) order.setShippedAt(now);    }
                case DELIVERED  -> { if (order.getDeliveredAt()  == null) order.setDeliveredAt(now);  }
                case CANCELLED  -> { if (order.getCancelledAt()  == null) order.setCancelledAt(now);  }
                case PLACED     -> { /* placedAt is set on order creation */ }
            }
        }

        // Tracking info — only overwrite when the admin actually supplied a value.
        // (Empty strings from the form fall through unchanged so admins can leave
        //  fields blank in subsequent status updates.)
        if (notBlank(req.getTrackingNumber()))      order.setTrackingNumber(req.getTrackingNumber().trim());
        if (notBlank(req.getCourier()))             order.setCourier(req.getCourier().trim());
        if (notBlank(req.getTrackingUrl()))         order.setTrackingUrl(req.getTrackingUrl().trim());
        if (req.getExpectedDeliveryDate() != null)  order.setExpectedDeliveryDate(req.getExpectedDeliveryDate());

        order.setStatus(next);
        if (req.getPaymentStatus() != null) {
            order.setPaymentStatus(req.getPaymentStatus());
        }
        return OrderService.toResponse(orderRepository.save(order));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
