package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /* ================= Primary Key ================= */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /* ================= User Info ================= */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(length = 15)
    private String phone;

    /* ================= Account Status ================= */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;

    /* ================= Address Mapping ================= */
    @OneToMany(
            mappedBy = "user",            // Address.user
            cascade = CascadeType.ALL,    // Save/Delete addresses with user
            orphanRemoval = true          // Remove address if detached
    )
    private List<Address> addresses;

    /* ================= Lifecycle ================= */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isAdmin == null) isAdmin = false;
    }
}
