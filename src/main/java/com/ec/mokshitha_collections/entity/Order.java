package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Embedded
    private AddressSnapshot shippingAddress;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 16)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ---------- Per-status timestamps (stamped by AdminOrderService.updateStatus) ---------- */
    @Column(name = "confirmed_at")  private LocalDateTime confirmedAt;
    @Column(name = "processing_at") private LocalDateTime processingAt;
    @Column(name = "shipped_at")    private LocalDateTime shippedAt;
    @Column(name = "delivered_at")  private LocalDateTime deliveredAt;
    @Column(name = "cancelled_at")  private LocalDateTime cancelledAt;

    /* ---------- Shipping / tracking (admin sets when status moves to SHIPPED) ---------- */
    @Column(name = "tracking_number", length = 64)
    private String trackingNumber;

    @Column(name = "courier", length = 64)
    private String courier;

    /** Full URL to the courier's tracking page for this shipment. */
    @Column(name = "tracking_url", columnDefinition = "TEXT")
    private String trackingUrl;

    /** When the customer is expected to receive the parcel. */
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (placedAt == null) placedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Convenience: human-readable order number, e.g. OD0000000001. */
    public String getOrderNumber() {
        return orderId == null ? null : String.format("OD%010d", orderId);
    }
}
