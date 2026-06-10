package com.ec.mokshitha_collections.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time, idempotent fix for stale enum CHECK constraints on the {@code orders}
 * table. Hibernate generates {@code orders_payment_method_check} etc. from the
 * enum values present when the column was first created. {@code ddl-auto=update}
 * never refreshes those constraints, so adding a new enum value (e.g.
 * {@code PaymentMethod.ONLINE}) would otherwise be rejected by the old check.
 *
 * Dropping them is safe — the application only ever writes valid enum names
 * (Hibernate {@code @Enumerated(STRING)}), so the DB-level check is redundant.
 * Postgres-specific; failures are logged, never fatal.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class OrdersSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrdersSchemaMigration.class);

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        // payment_method is the one that breaks today (COD -> COD/ONLINE); the
        // others are dropped proactively so future enum additions don't break.
        drop("orders_payment_method_check");
        drop("orders_payment_status_check");
        drop("orders_status_check");
    }

    private void drop(String constraint) {
        try {
            jdbc.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS " + constraint);
        } catch (Exception e) {
            log.warn("Could not drop constraint {} (ok to ignore on non-Postgres / fresh DB): {}",
                    constraint, e.getMessage());
        }
    }
}
