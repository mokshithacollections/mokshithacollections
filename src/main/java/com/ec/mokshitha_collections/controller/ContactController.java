package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.dto.contact.ContactRequest;
import com.ec.mokshitha_collections.service.EnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoint backing the contact form. Saves an enquiry for admin review. */
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EnquiryService enquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse> submit(@Valid @RequestBody ContactRequest req) {
        enquiryService.submit(req);
        return ResponseEntity.ok(ApiResponse.success("Thank you! We'll get back to you soon."));
    }
}
