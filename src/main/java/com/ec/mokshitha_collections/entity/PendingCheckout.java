package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a checkout's state between "Pay" and payment confirmation (Strategy A).
 * Stock is already reserved (decremented) when this row is created; the real
 * {@link Order} is built from this snapshot only once payment is verified, and
 * the reservation is released if the payment fails or the hold expires.
 */
@Entity
@Table(name = "pending_checkouts", indexes = {
        @Index(name = "idx_pending_rzp_order", columnList = "razorpay_order_id"),
        @Index(name = "idx_pending_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_id")
    private Long pendingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 64)
    private String razorpayOrderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Embedded
    private AddressSnapshot shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PendingCheckoutStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "pendingCheckout", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PendingCheckoutItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = PendingCheckoutStatus.HELD;
    }
}
