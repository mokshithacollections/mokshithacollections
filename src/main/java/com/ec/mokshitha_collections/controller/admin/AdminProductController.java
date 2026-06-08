package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.admin.ImageUploadResponse;
import com.ec.mokshitha_collections.dto.admin.ProductCreateRequest;
import com.ec.mokshitha_collections.dto.admin.ProductUpdateRequest;
import com.ec.mokshitha_collections.dto.admin.VariantCreateRequest;
import com.ec.mokshitha_collections.dto.admin.VariantUpdateRequest;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.dto.product.ProductDetailResponse;
import com.ec.mokshitha_collections.dto.product.ProductSummaryResponse;
import com.ec.mokshitha_collections.service.admin.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Function;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService service;

    /* ---------- Products ---------- */

    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = service.listAll(pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PostMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ProductUpdateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /** Make the product visible/purchasable. */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        service.setActive(id, true);
        return ResponseEntity.ok(ApiResponse.success("Product activated"));
    }

    /** Hide the product from the storefront (without deleting it). */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        service.setActive(id, false);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated"));
    }

    /** Permanently delete the product + variants, images, reviews, cart/wishlist refs. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        service.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted"));
    }

    /** Hero image upload from the admin's computer (multipart). */
    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ResponseEntity<ProductDetailResponse> uploadHero(@PathVariable Long id,
                                                            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadHeroImage(id, file));
    }

    /* ---------- Variants ---------- */

    @PostMapping("/{id}/variants")
    public ResponseEntity<ProductDetailResponse> addVariant(@PathVariable Long id,
                                                            @Valid @RequestBody VariantCreateRequest req) {
        return ResponseEntity.ok(service.addVariant(id, req));
    }

    @PostMapping("/{id}/variants/{variantId}")
    public ResponseEntity<ProductDetailResponse> updateVariant(@PathVariable Long id,
                                                               @PathVariable Long variantId,
                                                               @Valid @RequestBody VariantUpdateRequest req) {
        return ResponseEntity.ok(service.updateVariant(id, variantId, req));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    public ResponseEntity<ApiResponse> deleteVariant(@PathVariable Long id,
                                                     @PathVariable Long variantId) {
        service.deleteVariant(id, variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant deleted"));
    }

    /* ---------- Images ---------- */

    @PostMapping(value = "/{id}/variants/{variantId}/images",
                 consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponse> uploadImage(@PathVariable Long id,
                                                           @PathVariable Long variantId,
                                                           @RequestParam("file") MultipartFile file,
                                                           @RequestParam(required = false) String viewType,
                                                           @RequestParam(required = false) Boolean isPrimary) {
        return ResponseEntity.ok(service.uploadVariantImage(id, variantId, file, viewType, isPrimary));
    }

    @PostMapping("/images/{imageId}/primary")
    public ResponseEntity<ApiResponse> setPrimaryImage(@PathVariable Long imageId) {
        service.setPrimaryImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated"));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable Long imageId) {
        service.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted"));
    }
}
