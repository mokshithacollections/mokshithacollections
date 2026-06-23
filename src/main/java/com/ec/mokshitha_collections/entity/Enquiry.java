package com.ec.mokshitha_collections.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A message submitted through the public contact form. Admins review these
 * under Admin → Enquiries (filter New/Seen, delete).
 */
@Entity
@Table(name = "enquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enquiry_id")
    private Long enquiryId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 60)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Whether the visitor ticked the newsletter opt-in. */
    @Column(name = "newsletter_opt_in")
    private Boolean newsletter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnquiryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = EnquiryStatus.NEW;
    }
}
