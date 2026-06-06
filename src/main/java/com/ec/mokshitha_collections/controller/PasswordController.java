package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.auth.PasswordChangeRequest;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/password")
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @ModelAttribute PasswordChangeRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {

        passwordService.changePassword(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }
}
