package com.ec.mokshitha_collections.service.product;

import com.ec.mokshitha_collections.entity.Product;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Composable JPA Criteria fragments. Combine with `Specification.where(...).and(...)`.
 * Each fragment treats null inputs as "no filter" so they can be chained
 * unconditionally from the controller layer.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> isActive() {
        return (root, q, cb) -> cb.isTrue(root.get("isActive"));
    }

    /**
     * Matches the category itself OR any of its direct child categories, so that
     * selecting a parent (e.g. "Sarees") rolls up products filed under its
     * children ("Odisha Sarees", "Banarasi Sarees"). The parent join is LEFT so
     * products in a top-level category (no parent) are still matched.
     */
    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, q, cb) -> {
            if (categoryId == null) return null;
            var category = root.join("category", JoinType.INNER);
            var parent = category.join("parent", JoinType.LEFT);
            return cb.or(
                    cb.equal(category.get("categoryId"), categoryId),
                    cb.equal(parent.get("categoryId"), categoryId));
        };
    }

    /** Slug equivalent of {@link #hasCategory}: matches the category or its children. */
    public static Specification<Product> hasCategorySlug(String slug) {
        return (root, q, cb) -> {
            if (slug == null || slug.isBlank()) return null;
            var category = root.join("category", JoinType.INNER);
            var parent = category.join("parent", JoinType.LEFT);
            return cb.or(
                    cb.equal(category.get("slug"), slug),
                    cb.equal(parent.get("slug"), slug));
        };
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, q, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, q, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("price"), max);
    }

    /** Case-insensitive LIKE on name OR sku. */
    public static Specification<Product> matches(String search) {
        return (root, q, cb) -> {
            if (search == null || search.isBlank()) return null;
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("sku")), pattern));
        };
    }

    public static Specification<Product> isFeatured(Boolean featured) {
        return (root, q, cb) -> featured == null ? null : cb.equal(root.get("isFeatured"), featured);
    }
}
