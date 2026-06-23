package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.service.EnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin actions on contact-form enquiries. Listing is server-rendered by AdminPageController. */
@RestController
@RequestMapping("/api/admin/enquiries")
@RequiredArgsConstructor
public class AdminEnquiryController {

    private final EnquiryService enquiryService;

    /** Mark a NEW enquiry as SEEN. */
    @PostMapping("/{id}/seen")
    public ResponseEntity<ApiResponse> markSeen(@PathVariable Long id) {
        enquiryService.markSeen(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as seen"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        enquiryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry deleted"));
    }
}
