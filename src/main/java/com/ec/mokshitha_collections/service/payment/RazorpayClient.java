package com.ec.mokshitha_collections.service.payment;

import com.ec.mokshitha_collections.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Thin Razorpay client using their REST API directly (no SDK dependency) plus
 * JDK crypto for signature checks. Keeps the secret server-side only.
 */
@Component
public class RazorpayClient {

    private final RestClient rest = RestClient.create();

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    public String getKeyId() {
        return keyId;
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    /**
     * Creates a Razorpay order and returns its id (rzp order id). Amount is in
     * the smallest currency unit (paise for INR).
     */
    @SuppressWarnings("unchecked")
    public String createOrder(long amountPaise, String currency, String receipt) {
        if (!isConfigured()) {
            throw new BadRequestException("Online payments are not configured. Please use Cash on Delivery.");
        }
        String basic = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        try {
            Map<String, Object> body = rest.post()
                    .uri("https://api.razorpay.com/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", amountPaise,
                            "currency", currency,
                            "receipt", receipt,
                            "payment_capture", 1))
                    .retrieve()
                    .body(Map.class);
            Object id = body != null ? body.get("id") : null;
            if (id == null) {
                throw new BadRequestException("Could not start the payment. Please try again.");
            }
            return id.toString();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not reach the payment gateway. Please try again.");
        }
    }

    /** Verifies the browser checkout callback: HMAC_SHA256(order_id|payment_id, secret). */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (signature == null) return false;
        String expected = hmacSha256Hex(orderId + "|" + paymentId, keySecret);
        return constantTimeEquals(expected, signature);
    }

    /** Verifies a webhook delivery: HMAC_SHA256(rawBody, webhookSecret). */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (signature == null || webhookSecret == null || webhookSecret.isBlank()) return false;
        String expected = hmacSha256Hex(rawBody, webhookSecret);
        return constantTimeEquals(expected, signature);
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
