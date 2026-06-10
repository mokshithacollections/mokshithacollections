package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.payment.PaymentService;
import com.ec.mokshitha_collections.service.payment.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayClient razorpay;

    /** Reserve stock + open a Razorpay order; returns data to launch the widget. */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestParam Long addressId,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(paymentService.createPayment(principal.getUserId(), addressId));
    }

    /** Browser callback after a successful payment — verify signature, then confirm. */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_signature") String signature,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (!razorpay.verifyPaymentSignature(orderId, paymentId, signature)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Payment verification failed"));
        }
        Long createdOrderId = paymentService.confirm(orderId, paymentId);
        return ResponseEntity.ok(Map.of("status", "success", "orderId", createdOrderId));
    }

    /**
     * Frees the reserved stock when the customer closes/abandons the Razorpay
     * widget or it times out, so the items aren't locked until the hold expires.
     * No-op if the checkout was already paid/confirmed (only HELD ones release).
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@RequestParam("razorpay_order_id") String orderId) {
        paymentService.release(orderId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Razorpay server-to-server webhook (configure it in the Razorpay dashboard).
     * The reliable source of truth — confirms even if the browser never returns.
     * Must be CSRF-exempt and public; we verify the HMAC signature instead.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String rawBody,
                                          @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (!razorpay.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Razorpay webhook with invalid/missing signature ignored");
            return ResponseEntity.badRequest().body("invalid signature");
        }
        try {
            Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(rawBody);
            String event = str(root.get("event"));
            if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                String orderId = str(nav(root, "payload", "payment", "entity", "order_id"));
                String paymentId = str(nav(root, "payload", "payment", "entity", "id"));
                if (orderId == null) {
                    orderId = str(nav(root, "payload", "order", "entity", "id"));
                }
                if (orderId != null) paymentService.confirm(orderId, paymentId);
            } else if ("payment.failed".equals(event)) {
                String orderId = str(nav(root, "payload", "payment", "entity", "order_id"));
                if (orderId != null) paymentService.release(orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook", e);
            // Still 200 so Razorpay doesn't retry-storm on unparseable noise.
        }
        return ResponseEntity.ok("ok");
    }

    /** Safely walk nested JSON maps produced by Spring Boot's JsonParser. */
    private static Object nav(Object node, String... keys) {
        Object cur = node;
        for (String k : keys) {
            if (!(cur instanceof Map<?, ?> m)) return null;
            cur = m.get(k);
        }
        return cur;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
