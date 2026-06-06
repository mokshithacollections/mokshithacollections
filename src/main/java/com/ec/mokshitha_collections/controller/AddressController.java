package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.address.AddressRequest;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account/address")
public class AddressController {

    private final AddressService addressService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addAddress(
            @Valid @ModelAttribute AddressRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        addressService.addAddress(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.success("Address added"));
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateAddress(
            @PathVariable("id") Long addressId,
            @Valid @ModelAttribute AddressRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        addressService.updateAddress(principal.getUserId(), addressId, req);
        return ResponseEntity.ok(ApiResponse.success("Address updated"));
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteAddress(
            @PathVariable("id") Long addressId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        addressService.deleteAddress(principal.getUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted"));
    }

    @PostMapping("/default/{id}")
    public ResponseEntity<ApiResponse> setDefault(
            @PathVariable("id") Long addressId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        addressService.setDefaultAddress(principal.getUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated"));
    }
}
