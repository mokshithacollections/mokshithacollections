package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.auth.PasswordChangeRequest;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin self-service account actions. Lives under /api/admin/** so it's already
 * gated to ROLE_ADMIN by SecurityConfig. The password change reuses the shared
 * {@link PasswordService} (verifies the current password before updating).
 */
@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final PasswordService passwordService;

    @PostMapping("/password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody PasswordChangeRequest req,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        passwordService.changePassword(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }
}
