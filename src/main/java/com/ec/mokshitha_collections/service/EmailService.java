package com.ec.mokshitha_collections.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Thin wrapper over JavaMailSender for the app's transactional emails
 * (currently the password-reset link). Sends HTML via Brevo SMTP.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:Mokshitha Collections}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Sends an HTML email. Throws on failure so the caller can react. */
    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (from != null && !from.isBlank()) {
                helper.setFrom(from, fromName);
            }
            mailSender.send(msg);
            log.info("Email sent to {} (subject: {})", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new IllegalStateException("Could not send email", e);
        }
    }

    /** The branded password-reset email body. */
    public String buildResetEmail(String firstName, String resetLink, int expiryMinutes) {
        String name = (firstName == null || firstName.isBlank()) ? "there" : firstName;
        return """
            <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;color:#333;">
              <div style="background:linear-gradient(135deg,#55000c,#880012);padding:22px 24px;border-radius:12px 12px 0 0;">
                <h1 style="margin:0;color:#fff;font-size:1.4rem;">Mokshitha Collections</h1>
              </div>
              <div style="border:1px solid #eee;border-top:none;padding:24px;border-radius:0 0 12px 12px;">
                <p>Hi %s,</p>
                <p>We received a request to reset your password. Click the button below to set a new one:</p>
                <p style="text-align:center;margin:28px 0;">
                  <a href="%s" style="background:#880012;color:#fff;text-decoration:none;padding:13px 28px;border-radius:8px;font-weight:bold;display:inline-block;">Reset Password</a>
                </p>
                <p style="font-size:0.9rem;color:#666;">This link is valid for %d minutes and can be used once.
                   If you didn't request this, you can safely ignore this email — your password won't change.</p>
                <p style="font-size:0.8rem;color:#999;word-break:break-all;">If the button doesn't work, paste this link into your browser:<br>%s</p>
              </div>
            </div>
            """.formatted(name, resetLink, expiryMinutes, resetLink);
    }

    /* ===================== Lifecycle notifications (async, best-effort) =====================
       Run on a separate thread and never throw — a mail hiccup must not break
       registration/checkout/status changes. Callers pass already-loaded values
       (strings), so there's no lazy-loading across the async boundary. */

    @Async
    public void sendWelcome(String to, String firstName) {
        String name = safe(firstName);
        String body = shell("Welcome to Mokshitha Collections",
                "<p>Hi " + name + ",</p>" +
                "<p>Your account has been created successfully. Welcome to <b>Mokshitha Collections</b> — " +
                "we're delighted to have you!</p>" +
                "<p>Explore our latest sarees and dresses and enjoy a graceful shopping experience.</p>");
        quietSend(to, "Welcome to Mokshitha Collections 🎉", body);
    }

    @Async
    public void sendOrderPlaced(String to, String firstName, String orderNumber, BigDecimal total) {
        String body = shell("Order Placed",
                "<p>Hi " + safe(firstName) + ",</p>" +
                "<p>Thank you! We've received your order <b>" + orderNumber + "</b>.</p>" +
                row("Order", orderNumber) + row("Order Total", "₹" + total) +
                "<p>We'll email you as your order is confirmed, shipped and delivered.</p>");
        quietSend(to, "Your Mokshitha order " + orderNumber + " is placed", body);
    }

    /**
     * One email for each admin status change. {@code status} is the OrderStatus
     * name (CONFIRMED / SHIPPED / DELIVERED / CANCELLED). Shipping fields are used
     * only for SHIPPED.
     */
    @Async
    public void sendOrderStatus(String to, String firstName, String orderNumber, String status,
                                String courier, String trackingNumber, String trackingUrl,
                                LocalDate expectedDelivery) {
        String name = safe(firstName);
        String subject;
        String intro;
        String extra = "";
        switch (status) {
            case "CONFIRMED" -> {
                subject = "Your order " + orderNumber + " is confirmed";
                intro = "Good news! Your order <b>" + orderNumber + "</b> has been confirmed and is being prepared.";
            }
            case "SHIPPED" -> {
                subject = "Your order " + orderNumber + " has shipped";
                intro = "Your order <b>" + orderNumber + "</b> is on its way! 🚚";
                if (notBlank(courier))        extra += row("Courier", courier);
                if (notBlank(trackingNumber)) extra += row("Tracking No.", trackingNumber);
                if (expectedDelivery != null) extra += row("Expected Delivery", expectedDelivery.toString());
                if (notBlank(trackingUrl)) {
                    extra += "<p style=\"text-align:center;margin:22px 0;\">" +
                             "<a href=\"" + trackingUrl + "\" style=\"background:#880012;color:#fff;text-decoration:none;" +
                             "padding:12px 26px;border-radius:8px;font-weight:bold;display:inline-block;\">Track Shipment</a></p>";
                }
            }
            case "DELIVERED" -> {
                subject = "Your order " + orderNumber + " has been delivered";
                intro = "Your order <b>" + orderNumber + "</b> has been delivered. We hope you love it! 💝";
            }
            case "CANCELLED" -> {
                subject = "Your order " + orderNumber + " has been cancelled";
                intro = "Your order <b>" + orderNumber + "</b> has been cancelled. " +
                        "If you paid online, your refund will be processed to the original payment method.";
            }
            default -> { return; } // other statuses (e.g. PROCESSING) aren't emailed
        }
        String body = shell("Order Update", "<p>Hi " + name + ",</p><p>" + intro + "</p>" + extra);
        quietSend(to, subject, body);
    }

    /* ---------- small helpers ---------- */

    private void quietSend(String to, String subject, String html) {
        try {
            sendHtml(to, subject, html);
        } catch (Exception e) {
            log.error("Notification email to {} failed (subject: {}): {}", to, subject, e.getMessage());
        }
    }

    /** Branded wrapper shared by all lifecycle emails. */
    private static String shell(String heading, String innerHtml) {
        return """
            <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;color:#333;">
              <div style="background:linear-gradient(135deg,#55000c,#880012);padding:22px 24px;border-radius:12px 12px 0 0;">
                <h1 style="margin:0;color:#fff;font-size:1.3rem;">Mokshitha Collections</h1>
                <div style="color:#f6d99b;font-size:0.85rem;margin-top:2px;">%s</div>
              </div>
              <div style="border:1px solid #eee;border-top:none;padding:24px;border-radius:0 0 12px 12px;">
                %s
                <p style="margin-top:24px;font-size:0.8rem;color:#999;">— Team Mokshitha Collections</p>
              </div>
            </div>
            """.formatted(heading, innerHtml);
    }

    private static String row(String label, String value) {
        return "<p style=\"margin:4px 0;\"><span style=\"color:#666;\">" + label + ":</span> <b>" + value + "</b></p>";
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "there" : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
