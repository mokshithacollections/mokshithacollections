package com.ec.mokshitha_collections.service.impl;

import com.ec.mokshitha_collections.dto.category.CategoryResponse;
import com.ec.mokshitha_collections.dto.product.ProductDetailResponse;
import com.ec.mokshitha_collections.dto.product.ProductSummaryResponse;
import com.ec.mokshitha_collections.dto.product.VariantImageResponse;
import com.ec.mokshitha_collections.dto.product.VariantResponse;
import com.ec.mokshitha_collections.entity.Product;
import com.ec.mokshitha_collections.entity.ProductCategory;
import com.ec.mokshitha_collections.entity.ProductVariant;
import com.ec.mokshitha_collections.entity.ProductVariantImage;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductRepository;
import com.ec.mokshitha_collections.repository.ProductReviewRepository;
import com.ec.mokshitha_collections.repository.ProductVariantImageRepository;
import com.ec.mokshitha_collections.repository.ProductVariantRepository;
import com.ec.mokshitha_collections.service.ProductService;
import com.ec.mokshitha_collections.service.product.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;
    private final ProductReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Product> getFeaturedProducts() {
        return productRepository.findTop4ByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listProducts(Long categoryId,
                                                     String categorySlug,
                                                     BigDecimal minPrice,
                                                     BigDecimal maxPrice,
                                                     String search,
                                                     Boolean featured,
                                                     Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.isActive(),
                ProductSpecifications.hasCategory(categoryId),
                ProductSpecifications.hasCategorySlug(categorySlug),
                ProductSpecifications.priceAtLeast(minPrice),
                ProductSpecifications.priceAtMost(maxPrice),
                ProductSpecifications.matches(search),
                ProductSpecifications.isFeatured(featured));

        Page<Product> page = productRepository.findAll(spec, pageable);

        // Enrich each card with its distinct-colour count in one extra query
        // (avoids N+1 lazy loads while listing).
        List<Long> ids = page.getContent().stream().map(Product::getProductId).toList();
        Map<Long, Integer> colorCounts = ids.isEmpty() ? Map.of()
                : variantRepository.countDistinctColorsByProductIds(ids).stream()
                    .collect(Collectors.toMap(r -> (Long) r[0], r -> ((Long) r[1]).intValue()));

        return page.map(p -> toSummary(p, colorCounts.getOrDefault(p.getProductId(), 0)));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product p = productRepository.findActiveByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return buildDetail(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailForAdmin(Long productId) {
        // Admins must be able to open deactivated products (e.g. to re-activate),
        // so this path skips the isActive filter the customer path applies.
        Product p = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return buildDetail(p);
    }

    private ProductDetailResponse buildDetail(Product p) {
        Long productId = p.getProductId();
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        List<ProductVariantImage> allImages = variantImageRepository.findByProductId(productId);
        Map<Long, List<VariantImageResponse>> imagesByVariant = allImages.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getVariant().getVariantId(),
                        Collectors.mapping(ProductServiceImpl::toImageResponse, Collectors.toList())));

        List<VariantResponse> variantDtos = variants.stream()
                .map(v -> VariantResponse.builder()
                        .variantId(v.getVariantId())
                        .color(v.getColor())
                        .size(v.getSize())
                        .stockQuantity(v.getStockQuantity())
                        .skuVariant(v.getSkuVariant())
                        .images(imagesByVariant.getOrDefault(v.getVariantId(), List.of()))
                        .build())
                .toList();

        Double avg = reviewRepository.averageRatingForProduct(productId);
        long count = reviewRepository.countApprovedForProduct(productId);

        return ProductDetailResponse.builder()
                .productId(p.getProductId())
                .name(p.getName())
                .description(p.getDescription())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .imageUrl(p.getImageUrl())
                .fabricType(p.getFabricType())
                .occasion(p.getOccasion())
                .isFeatured(p.getIsFeatured())
                .isBestseller(p.getIsBestseller())
                .isTrending(p.getIsTrending())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .category(toCategoryResponse(p.getCategory()))
                .variants(variantDtos)
                .averageRating(avg)
                .reviewCount(count)
                .build();
    }

    public static ProductSummaryResponse toSummary(Product p) {
        return toSummary(p, null);
    }

    public static ProductSummaryResponse toSummary(Product p, Integer colorCount) {
        ProductCategory c = p.getCategory();
        return ProductSummaryResponse.builder()
                .productId(p.getProductId())
                .name(p.getName())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .imageUrl(p.getImageUrl())
                .categoryName(c != null ? c.getName() : null)
                .categorySlug(c != null ? c.getSlug() : null)
                .isFeatured(p.getIsFeatured())
                .isBestseller(p.getIsBestseller())
                .isTrending(p.getIsTrending())
                .colorCount(colorCount)
                .build();
    }

    private static VariantImageResponse toImageResponse(ProductVariantImage img) {
        return VariantImageResponse.builder()
                .imageId(img.getImageId())
                .imageUrl(img.getImageUrl())
                .viewType(img.getViewType())
                .isPrimary(img.getIsPrimary())
                .build();
    }

    private static CategoryResponse toCategoryResponse(ProductCategory c) {
        if (c == null) return null;
        return CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .parentId(c.getParent() != null ? c.getParent().getCategoryId() : null)
                .build();
    }
}
