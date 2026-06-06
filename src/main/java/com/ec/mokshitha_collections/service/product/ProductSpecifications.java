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

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, q, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("category").get("categoryId"), categoryId);
    }

    public static Specification<Product> hasCategorySlug(String slug) {
        return (root, q, cb) -> {
            if (slug == null || slug.isBlank()) return null;
            return cb.equal(root.join("category", JoinType.INNER).get("slug"), slug);
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
