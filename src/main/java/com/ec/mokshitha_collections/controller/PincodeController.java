package com.ec.mokshitha_collections.controller;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Server-side proxy for India Post's public PIN-code lookup. Calling it from the
 * browser directly can be blocked by CORS, so the address form hits this
 * same-origin endpoint instead. Returns {found, city, state}.
 */
@RestController
@RequestMapping("/api/pincode")
public class PincodeController {

    private final RestClient restClient = RestClient.create();

    @GetMapping("/{pin}")
    public ResponseEntity<Map<String, Object>> lookup(@PathVariable String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            return ResponseEntity.ok(Map.of("found", false, "message", "Enter a valid 6-digit PIN"));
        }
        try {
            List<Map<String, Object>> resp = restClient.get()
                    .uri("https://api.postalpincode.in/pincode/{pin}", pin)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (resp != null && !resp.isEmpty()) {
                Map<String, Object> first = resp.get(0);
                if ("Success".equals(String.valueOf(first.get("Status")))
                        && first.get("PostOffice") instanceof List<?> offices
                        && !offices.isEmpty()
                        && offices.get(0) instanceof Map<?, ?> po) {
                    return ResponseEntity.ok(Map.of(
                            "found", true,
                            "city", String.valueOf(po.get("District")),
                            "state", String.valueOf(po.get("State"))
                    ));
                }
            }
            return ResponseEntity.ok(Map.of("found", false, "message", "PIN not found"));
        } catch (Exception e) {
            // Network/parse failure — let the UI fall back to manual entry.
            return ResponseEntity.ok(Map.of("found", false, "message", "Lookup unavailable"));
        }
    }
}
