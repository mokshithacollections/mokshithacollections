package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One cart per user. Lines live in CartItem (variant + quantity).
 *
 * NOTE: this entity used to have a `product` field tied to a NOT NULL column.
 * If your existing dev DB still has that column, drop the table once
 * (the rows are throwaway dev data) so Hibernate can recreate it cleanly:
 *
 *     DROP TABLE cart_items;  DROP TABLE user_carts;
 */
@Entity
@Table(name = "user_carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
