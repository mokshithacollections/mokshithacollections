package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.admin.OfferRequest;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.dto.offer.OfferResponse;
import com.ec.mokshitha_collections.service.admin.AdminOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
@RequiredArgsConstructor
public class AdminOfferController {

    private final AdminOfferService service;

    @GetMapping
    public ResponseEntity<List<OfferResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody OfferRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PostMapping("/{id}")
    public ResponseEntity<OfferResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody OfferRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        service.setActive(id, true);
        return ResponseEntity.ok(ApiResponse.success("Offer activated"));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        service.setActive(id, false);
        return ResponseEntity.ok(ApiResponse.success("Offer deactivated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Offer deleted"));
    }
}
