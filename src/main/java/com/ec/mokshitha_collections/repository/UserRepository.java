package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.addresses
            WHERE u.userId = :userId
            """)
    Optional<User> findByIdWithAddresses(@Param("userId") Long userId);

    /**
     * Admin search query. NEVER call this with a null/blank `q` — use
     * {@link #search(String, Pageable)} below, which branches to findAll
     * when there's no search term. Calling this with null produces a
     * Postgres "lower(bytea) does not exist" error because the driver
     * can't infer the parameter's SQL type.
     */
    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<User> searchByQuery(@Param("q") String q, Pageable pageable);

    /** Public search entry point: handles the null/blank case in Java. */
    default Page<User> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return findAll(pageable);
        return searchByQuery(q.trim(), pageable);
    }

    long countByIsAdminTrue();
}
