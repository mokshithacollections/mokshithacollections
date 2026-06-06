package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.admin.UserAdminResponse;
import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public ResponseEntity<PageResponse<UserAdminResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = service.list(search, pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<UserAdminResponse> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails caller) {
        return ResponseEntity.ok(service.setActive(id, true, caller.getUserId()));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserAdminResponse> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails caller) {
        return ResponseEntity.ok(service.setActive(id, false, caller.getUserId()));
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<UserAdminResponse> promote(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails caller) {
        return ResponseEntity.ok(service.setAdmin(id, true, caller.getUserId()));
    }

    @PostMapping("/{id}/demote")
    public ResponseEntity<UserAdminResponse> demote(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails caller) {
        return ResponseEntity.ok(service.setAdmin(id, false, caller.getUserId()));
    }
}
