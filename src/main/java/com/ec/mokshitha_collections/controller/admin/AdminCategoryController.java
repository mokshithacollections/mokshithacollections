package com.ec.mokshitha_collections.controller.admin;

import com.ec.mokshitha_collections.dto.admin.CategoryCreateRequest;
import com.ec.mokshitha_collections.dto.admin.CategoryUpdateRequest;
import com.ec.mokshitha_collections.dto.category.CategoryResponse;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.service.admin.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService service;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PostMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryUpdateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /** Make the category visible to customers. */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        service.setActive(id, true);
        return ResponseEntity.ok(ApiResponse.success("Category activated"));
    }

    /** Hide the category from customers (without deleting it). */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        service.setActive(id, false);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated"));
    }

    /** Permanently delete the category AND all products under it. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        service.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Category and its products deleted"));
    }
}
