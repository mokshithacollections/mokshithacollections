package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.common.PageResponse;
import com.ec.mokshitha_collections.dto.product.ProductDetailResponse;
import com.ec.mokshitha_collections.dto.product.ProductSummaryResponse;
import com.ec.mokshitha_collections.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.function.Function;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean featured,
            @PageableDefault(size = 20) Pageable pageable) {

        var page = productService.listProducts(
                categoryId, categorySlug, minPrice, maxPrice, search, featured, pageable);
        return ResponseEntity.ok(PageResponse.of(page, Function.identity()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> get(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(productService.getProductDetail(productId));
    }
}
