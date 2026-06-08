package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.dto.admin.CategoryCreateRequest;
import com.ec.mokshitha_collections.dto.admin.CategoryUpdateRequest;
import com.ec.mokshitha_collections.dto.category.CategoryResponse;
import com.ec.mokshitha_collections.entity.Product;
import com.ec.mokshitha_collections.entity.ProductCategory;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.ConflictException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductCategoryRepository;
import com.ec.mokshitha_collections.repository.ProductRepository;
import com.ec.mokshitha_collections.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AdminProductService adminProductService;

    /** Lists ALL categories (active + inactive) for admin grid. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest req) {
        if (categoryRepository.findBySlug(req.getSlug()).isPresent()) {
            throw new ConflictException("Slug already in use");
        }
        ProductCategory parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
        }

        ProductCategory c = ProductCategory.builder()
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .parent(parent)
                .isActive(req.getIsActive() == null ? true : req.getIsActive())
                .build();
        return CategoryService.toResponse(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse update(Long categoryId, CategoryUpdateRequest req) {
        ProductCategory c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (req.getName() != null) c.setName(req.getName());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getIsActive() != null) c.setIsActive(req.getIsActive());

        if (req.getSlug() != null && !req.getSlug().equals(c.getSlug())) {
            if (categoryRepository.findBySlug(req.getSlug()).isPresent()) {
                throw new ConflictException("Slug already in use");
            }
            c.setSlug(req.getSlug());
        }

        if (req.getParentId() != null) {
            if (req.getParentId().equals(categoryId)) {
                throw new BadRequestException("A category cannot be its own parent");
            }
            ProductCategory parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            c.setParent(parent);
        }

        return CategoryService.toResponse(categoryRepository.save(c));
    }

    /** Toggle a category's visibility (Active/Inactive) without deleting it. */
    @Transactional
    public void setActive(Long categoryId, boolean active) {
        ProductCategory c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        c.setIsActive(active);
        categoryRepository.save(c);
    }

    /**
     * Hard delete: permanently removes the category AND every product under it
     * (each product's variants, images, reviews and cart/wishlist refs are
     * cleaned up by AdminProductService.hardDelete). Sub-categories are detached
     * (their parent link is cleared) so the row can be removed.
     */
    @Transactional
    public void hardDelete(Long categoryId) {
        ProductCategory c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        for (Product p : productRepository.findByCategoryCategoryId(categoryId)) {
            adminProductService.hardDelete(p.getProductId());
        }
        categoryRepository.detachChildren(categoryId);
        categoryRepository.delete(c);
    }
}
