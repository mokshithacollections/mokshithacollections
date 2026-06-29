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
                        && !offices.isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "found", true,
                            "city", resolveTown(offices),
                            "district", firstReal(offices, "District"),
                            "state", firstReal(offices, "State")
                    ));
                }
            }
            return ResponseEntity.ok(Map.of("found", false, "message", "PIN not found"));
        } catch (Exception e) {
            // Network/parse failure — let the UI fall back to manual entry.
            return ResponseEntity.ok(Map.of("found", false, "message", "Lookup unavailable"));
        }
    }

    /**
     * Best "town/city" for the PIN from India Post's post-office list, in order:
     *   1. the Head/General Post Office name (named after the city, e.g. "Ongole H.O"
     *      → Ongole, "New Delhi G.P.O." → New Delhi),
     *   2. otherwise the Sub Post Office name (the actual town/sub-town that the
     *      branch offices hang under, e.g. "Chimakurty" for PIN 523226) — this is
     *      what Flipkart/Amazon show,
     *   3. otherwise a real Block (taluk/mandal),
     *   4. otherwise the District,
     *   5. otherwise any post-office name.
     * Postal hierarchy is HO → SO → BO, so HO/SO beat the broader Block. The first
     * office's Block is often "NA", which is why we scan the whole list.
     */
    private static String resolveTown(List<?> offices) {
        String headTown = null, blockTown = null, district = null, anyName = null;
        java.util.List<String> subTowns = new java.util.ArrayList<>();
        for (Object o : offices) {
            if (!(o instanceof Map<?, ?> po)) continue;
            if (district == null && isReal(clean(po.get("District")))) district = clean(po.get("District"));

            String branch = String.valueOf(po.get("BranchType")).toLowerCase();
            String name = stripOfficeSuffix(clean(po.get("Name")));
            String block = clean(po.get("Block"));

            if (headTown == null && branch.contains("head") && isReal(name)) headTown = name;
            if (branch.contains("sub") && isReal(name)) subTowns.add(name);
            if (blockTown == null && isReal(block)) blockTown = block;
            if (anyName == null && isReal(name)) anyName = name;
        }
        if (isReal(headTown)) return headTown;
        // Exactly one Sub Post Office = a distinct town (e.g. Chimakurty for 523226).
        // Several SOs = localities of one city, so the Block is the real city
        // (e.g. Kurnool Road / Lawyer Pet / M.M.Donka all under Ongole, 523002).
        if (subTowns.size() == 1) return subTowns.get(0);
        if (isReal(blockTown)) return blockTown;
        if (!subTowns.isEmpty()) return subTowns.get(0);
        if (isReal(district)) return district;
        return isReal(anyName) ? anyName : "";
    }

    /** Drop a trailing office-type token: "Ongole H.O" → "Ongole", "Lawyer Pet (O)" → "Lawyer Pet". */
    private static String stripOfficeSuffix(String name) {
        if (name == null) return "";
        return name.replaceAll("(?i)\\s*(\\bH\\.?O\\b|\\bS\\.?O\\b|\\bB\\.?O\\b|\\bG\\.?P\\.?O\\b|GPO|\\(O\\)|Bazar|Bazaar)\\.?$", "").trim();
    }

    /** First non-blank value of {@code key} across all post offices. */
    private static String firstReal(List<?> offices, String key) {
        for (Object o : offices) {
            if (o instanceof Map<?, ?> po) {
                String v = clean(po.get(key));
                if (isReal(v)) return v;
            }
        }
        return "";
    }

    private static String clean(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static boolean isReal(String s) {
        return s != null && !s.isEmpty() && !"NA".equalsIgnoreCase(s) && !"null".equalsIgnoreCase(s);
    }
}
