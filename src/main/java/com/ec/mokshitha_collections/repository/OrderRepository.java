package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.Order;
import com.ec.mokshitha_collections.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    /** Detail fetch with items, used by /api/orders/{id}. */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.orderId = :orderId AND o.user.userId = :userId
            """)
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    /* ---------- Admin queries ---------- */

    Page<Order> findAllByOrderByPlacedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByPlacedAtDesc(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.orderId = :orderId
            """)
    Optional<Order> findByIdWithItems(Long orderId);

    /** Look up the order created for a given Razorpay order (idempotent confirm). */
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    /* ---------- Per-user stats for the admin user-detail page ---------- */

    long countByUserUserId(Long userId);

    /**
     * Sum of order totals for a user, excluding orders with the given status
     * (typically CANCELLED). Returns null when there are no matching rows —
     * callers should use {@link #sumTotalSpentByUserIdOrZero(Long)} to get
     * a guaranteed BigDecimal.
     *
     * Status is bound as a parameter (rather than written inline as
     * `OrderStatus.CANCELLED`) because Hibernate 7's JPQL parser rejects
     * the fully-qualified enum literal in some setups.
     *
     * SUM(...) intentionally has no COALESCE — that wrapper made Hibernate
     * return an Integer/Long instead of a BigDecimal, breaking the
     * return-type conversion.
     */
    @Query("""
            SELECT SUM(o.totalAmount)
            FROM Order o
            WHERE o.user.userId = :userId
              AND o.status <> :excludedStatus
            """)
    BigDecimal sumTotalSpentByUserId(@Param("userId") Long userId,
                                     @Param("excludedStatus") OrderStatus excludedStatus);

    /** Null-safe wrapper that always returns a BigDecimal (zero when no orders). */
    default BigDecimal sumTotalSpentByUserIdOrZero(Long userId) {
        BigDecimal total = sumTotalSpentByUserId(userId, OrderStatus.CANCELLED);
        return total != null ? total : BigDecimal.ZERO;
    }
}
