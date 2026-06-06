package com.ec.mokshitha_collections.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Active only under the prod profile. Refuses to let the app start if any of
 * the security-critical secrets are still set to the dev defaults.
 *
 * Adding a new secret to the project? Wire it in here so prod boots will
 * fail fast rather than running with a known-bad value.
 */
@Component
@Profile("prod")
@Slf4j
public class StartupSecretsValidator {

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.remember-me.key:}")
    private String rememberMeKey;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @PostConstruct
    void validate() {
        List<String> bad = new ArrayList<>();

        if ("postgres".equals(dbPassword) || dbPassword.isBlank()) {
            bad.add("DB_PASSWORD (current is the dev default or empty)");
        }
        if (rememberMeKey.isBlank() || rememberMeKey.startsWith("dev-only-")) {
            bad.add("REMEMBER_ME_KEY (current is the dev default or empty)");
        }
        if ("ChangeMe!2026".equals(adminPassword) || adminPassword.isBlank()) {
            bad.add("ADMIN_PASSWORD (current is the dev default or empty)");
        }

        if (!bad.isEmpty()) {
            String msg = "Refusing to start under 'prod' profile — these secrets must be set:\n  - "
                    + String.join("\n  - ", bad);
            log.error("============================================================\n{}\n============================================================", msg);
            throw new IllegalStateException(msg);
        }
        log.info("Production secrets validated.");
    }
}
