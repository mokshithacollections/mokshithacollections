package com.ec.mokshitha_collections.repository;

import com.ec.mokshitha_collections.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByIsActiveTrueOrderByNameAsc();

    Optional<ProductCategory> findBySlug(String slug);

    /** Null out the parent link on any sub-categories so a parent can be deleted. */
    @Modifying
    @Query("UPDATE ProductCategory c SET c.parent = null WHERE c.parent.categoryId = :parentId")
    void detachChildren(Long parentId);
}
