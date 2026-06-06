package com.ec.mokshitha_collections.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login throttling: 5 failed attempts in a 15-minute window per IP
 * blocks the IP for the rest of that window.
 *
 * Single-instance only — if you scale horizontally, swap the backing map for
 * Redis or a JDBC store. Cleared on a process restart, which is fine for
 * brute-force defence (attacker has to start over too).
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Counter c = counters.get(ip);
        if (c == null) return false;
        if (Duration.between(c.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
            counters.remove(ip);
            return false;
        }
        return c.count >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        counters.compute(ip, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || Duration.between(existing.windowStart, now).compareTo(WINDOW) > 0) {
                return new Counter(1, now);
            }
            existing.count++;
            return existing;
        });
    }

    public void recordSuccess(String ip) {
        counters.remove(ip);
    }

    private static final class Counter {
        int count;
        final Instant windowStart;

        Counter(int count, Instant windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
