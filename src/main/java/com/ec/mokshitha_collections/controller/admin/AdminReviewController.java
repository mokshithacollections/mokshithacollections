package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.admin.PendingReviewResponse;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.service.admin.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService service;

    @GetMapping("/pending")
    public ResponseEntity<PageResponse<PendingReviewResponse>> pending(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = service.listPending(pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PendingReviewResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> reject(@PathVariable Long id) {
        service.reject(id);
        return ResponseEntity.ok(ApiResponse.success("Review rejected"));
    }
}
