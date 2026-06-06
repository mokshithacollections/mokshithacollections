package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.category.CategoryResponse;
import com.ec.mokshitha_collections.entity.ProductCategory;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(CategoryService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long categoryId) {
        ProductCategory c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        ProductCategory c = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(c);
    }

    public static CategoryResponse toResponse(ProductCategory c) {
        return CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .parentId(c.getParent() != null ? c.getParent().getCategoryId() : null)
                .build();
    }
}
