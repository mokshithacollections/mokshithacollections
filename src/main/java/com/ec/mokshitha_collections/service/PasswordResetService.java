package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.entity.PasswordResetToken;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.repository.PasswordResetTokenRepository;
import com.ec.mokshitha_collections.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Self-service password reset. Emails a single-use, time-limited link; only the
 * SHA-256 hash of the token is stored. All lookups respond generically so the
 * feature never reveals whether an email is registered (no user enumeration).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.password-reset.expiry-minutes:30}")
    private int expiryMinutes;

    /**
     * Issues a reset link for the email. Returns {@code true} if an account
     * exists (and a link was sent), {@code false} if the email isn't registered
     * — the caller uses this to show "email not registered".
     */
    @Transactional
    public boolean requestReset(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        Optional<User> maybeUser = userRepository.findByEmail(normalized);
        if (maybeUser.isEmpty()) {
            return false; // email not registered
        }
        User user = maybeUser.get();

        // Invalidate any earlier outstanding links, then mint a fresh one.
        tokenRepository.markAllUsedForUser(user.getUserId());

        String rawToken = randomToken();
        tokenRepository.save(PasswordResetToken.builder()
                .userId(user.getUserId())
                .tokenHash(sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build());

        String link = baseUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;
        try {
            emailService.sendHtml(user.getEmail(), "Reset your Mokshitha Collections password",
                    emailService.buildResetEmail(user.getFirstName(), link, expiryMinutes));
        } catch (Exception e) {
            // Log but still return normally so the response stays generic.
            log.error("Password-reset email failed for userId {}: {}", user.getUserId(), e.getMessage());
        }
        return true;
    }

    /** True if the raw token maps to a live (unused, unexpired) reset token. */
    @Transactional(readOnly = true)
    public boolean isValidToken(String rawToken) {
        return findLiveToken(rawToken).isPresent();
    }

    /** Completes the reset: validates the token + passwords, then updates the hash. */
    @Transactional
    public void reset(String rawToken, String newPassword, String confirmPassword) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        PasswordResetToken token = findLiveToken(rawToken)
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired."));

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private Optional<PasswordResetToken> findLiveToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> !t.isUsed() && t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
