package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByIsActiveTrueOrderByNameAsc();

    Optional<ProductCategory> findBySlug(String slug);
}
