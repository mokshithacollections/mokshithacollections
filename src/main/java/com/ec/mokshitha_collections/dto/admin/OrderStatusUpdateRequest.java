package com.ec.mokshitha_collections.dto.admin;

import com.ec.mokshitha_collections.entity.OrderStatus;
import com.ec.mokshitha_collections.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderStatusUpdateRequest {

    @NotNull
    private OrderStatus status;

    /** Optional — only set when payment progresses (e.g. COD becomes PAID on delivery). */
    private PaymentStatus paymentStatus;

    /* ---------- Shipping info — fill in when status is SHIPPED ---------- */

    @Size(max = 64, message = "Tracking number is too long")
    private String trackingNumber;

    @Size(max = 64, message = "Courier name is too long")
    private String courier;

    /** Direct URL the customer can click to track on the courier's site. */
    private String trackingUrl;

    /** Expected delivery date — admin estimate. */
    private LocalDate expectedDeliveryDate;
}
