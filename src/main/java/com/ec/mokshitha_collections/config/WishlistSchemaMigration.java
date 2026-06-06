package com.ec.mokshitha_collections.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time, idempotent cleanup so the wishlist can go variant-level on an
 * existing database without any manual SQL.
 *
 * Hibernate (ddl-auto=update) adds the new {@code variant_id} column but will
 * NOT drop the old UNIQUE(user_id, product_id) constraint — and that constraint
 * blocks saving the same product in two colours. This runner:
 *   1. drops any UNIQUE constraint left on user_wishlists (only the legacy one
 *      exists; the entity no longer declares any), and
 *   2. deletes legacy product-level rows that have no variant (they can't be
 *      mapped to a specific variant).
 *
 * Safe to run on every startup: after the first run there's nothing to do.
 * Postgres-specific (uses pg_constraint); failures are logged, never fatal.
 */
@Component
@RequiredArgsConstructor
public class WishlistSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WishlistSchemaMigration.class);

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> uniqueConstraints = jdbc.queryForList(
                    "SELECT conname FROM pg_constraint " +
                    "WHERE conrelid = 'user_wishlists'::regclass AND contype = 'u'",
                    String.class);
            for (String name : uniqueConstraints) {
                jdbc.execute("ALTER TABLE user_wishlists DROP CONSTRAINT IF EXISTS \"" + name + "\"");
                log.info("Dropped legacy wishlist unique constraint: {}", name);
            }

            int removed = jdbc.update("DELETE FROM user_wishlists WHERE variant_id IS NULL");
            if (removed > 0) {
                log.info("Removed {} legacy product-level wishlist row(s) without a variant", removed);
            }
        } catch (Exception e) {
            // Non-fatal: e.g. a non-Postgres DB or the table not existing yet.
            log.warn("Wishlist schema migration skipped: {}", e.getMessage());
        }
    }
}
