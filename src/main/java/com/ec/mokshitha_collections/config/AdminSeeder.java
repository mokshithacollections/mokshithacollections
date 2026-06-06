package com.ec.mokshitha_collections.config;

import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps an admin account on first startup so the admin endpoints in
 * Phase 6 are usable. Runs every startup but only inserts when no admin
 * exists, so it's idempotent.
 *
 * Configure via env vars:
 *   ADMIN_EMAIL     (default: admin@mokshitha.local)
 *   ADMIN_PASSWORD  (default: ChangeMe!2026 — replace immediately in any real env)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    @Value("${app.admin.email:admin@mokshitha.local}")
    private String adminEmail;

    @Value("${app.admin.password:ChangeMe!2026}")
    private String adminPassword;

    private static final String DEFAULT_PASSWORD = "ChangeMe!2026";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> Boolean.TRUE.equals(u.getIsAdmin()));

        if (adminExists) {
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Admin seed skipped: a user with email {} already exists but is not admin. " +
                    "Promote them manually.", email);
            return;
        }

        User admin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName("Admin")
                .lastName("")
                .isActive(true)
                .isAdmin(true)
                .build();
        userRepository.save(admin);

        if (DEFAULT_PASSWORD.equals(adminPassword)) {
            log.warn("=========================================================");
            log.warn(" Admin account created with the DEFAULT password.");
            log.warn(" Email:    {}", email);
            log.warn(" Password: {}", DEFAULT_PASSWORD);
            log.warn(" Change it now: log in and POST /account/password,");
            log.warn(" or set ADMIN_EMAIL / ADMIN_PASSWORD env vars before next start.");
            log.warn("=========================================================");
        } else {
            log.info("Admin account created for {}", email);
        }
    }
}
