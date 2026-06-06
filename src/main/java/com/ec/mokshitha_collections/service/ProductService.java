package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.product.ProductDetailResponse;
import com.ec.mokshitha_collections.dto.product.ProductSummaryResponse;
import com.ec.mokshitha_collections.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    /** Latest 4 featured + active products. Used by the home page hero strip; returns
     *  the Product entity since the home Thymeleaf template iterates entity fields. */
    List<Product> getFeaturedProducts();

    /** Paged + filtered listing for the shop / search / category pages. */
    Page<ProductSummaryResponse> listProducts(Long categoryId,
                                              String categorySlug,
                                              BigDecimal minPrice,
                                              BigDecimal maxPrice,
                                              String search,
                                              Boolean featured,
                                              Pageable pageable);

    /** Detail view: product + category + variants + images + review aggregates. */
    ProductDetailResponse getProductDetail(Long productId);

    /** Same detail view but for admin screens — includes deactivated products. */
    ProductDetailResponse getProductDetailForAdmin(Long productId);
}
