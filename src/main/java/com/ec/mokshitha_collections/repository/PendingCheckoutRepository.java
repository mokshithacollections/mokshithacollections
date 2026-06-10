package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.PendingCheckout;
import com.ec.mokshitha_collections.entity.PendingCheckoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PendingCheckoutRepository extends JpaRepository<PendingCheckout, Long> {

    @Query("""
            SELECT p FROM PendingCheckout p
            LEFT JOIN FETCH p.items
            WHERE p.razorpayOrderId = :razorpayOrderId
            """)
    Optional<PendingCheckout> findByRazorpayOrderId(String razorpayOrderId);

    /** HELD checkouts whose hold has expired — released by the scheduled job. */
    List<PendingCheckout> findByStatusAndExpiresAtBefore(PendingCheckoutStatus status, LocalDateTime cutoff);
}
