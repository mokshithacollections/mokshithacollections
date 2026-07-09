package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single-use, time-limited password-reset token. We store only the SHA-256
 * hash of the token (never the raw value), the same principle as passwords —
 * a DB leak can't be turned back into working reset links.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_reset_token_hash", columnList = "token_hash"),
        @Index(name = "idx_reset_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_id")
    private Long resetId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** SHA-256 hash (hex) of the raw token that was emailed. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
