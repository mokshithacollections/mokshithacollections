package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.UserCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserCartRepository extends JpaRepository<UserCart, Long> {

    Optional<UserCart> findByUserUserId(Long userId);

    /** Eager-loads cart + its items + each item's variant + product so a single
     * GET /cart response avoids N+1 hits. */
    @Query("""
            SELECT DISTINCT c FROM UserCart c
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.variant v
            LEFT JOIN FETCH v.product
            WHERE c.user.userId = :userId
            """)
    Optional<UserCart> findByUserIdWithItems(Long userId);
}
