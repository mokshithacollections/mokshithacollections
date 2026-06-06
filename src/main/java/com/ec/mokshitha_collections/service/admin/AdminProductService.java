package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.dto.admin.ImageUploadResponse;
import com.ec.mokshitha_collections.dto.admin.ProductCreateRequest;
import com.ec.mokshitha_collections.dto.admin.ProductUpdateRequest;
import com.ec.mokshitha_collections.dto.admin.VariantCreateRequest;
import com.ec.mokshitha_collections.dto.admin.VariantUpdateRequest;
import com.ec.mokshitha_collections.dto.product.ProductDetailResponse;
import com.ec.mokshitha_collections.dto.product.ProductSummaryResponse;
import com.ec.mokshitha_collections.entity.Product;
import com.ec.mokshitha_collections.entity.ProductCategory;
import com.ec.mokshitha_collections.entity.ProductVariant;
import com.ec.mokshitha_collections.entity.ProductVariantImage;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.ConflictException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.ProductCategoryRepository;
import com.ec.mokshitha_collections.repository.ProductRepository;
import com.ec.mokshitha_collections.repository.ProductVariantImageRepository;
import com.ec.mokshitha_collections.repository.ProductVariantRepository;
import com.ec.mokshitha_collections.service.ProductService;
import com.ec.mokshitha_collections.service.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository imageRepository;
    private final ImageStorageService imageStorageService;
    private final ProductService productService;

    /* ---------- Products ---------- */

    /** Lists ALL products (active + inactive) for the admin grid. */
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductServiceImpl::toSummary);
    }

    @Transactional
    public ProductDetailResponse create(ProductCreateRequest req) {
        if (productRepository.existsBySkuIgnoreCase(req.getSku())) {
            throw new ConflictException("SKU already exists");
        }
        ProductCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product p = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .discountPrice(req.getDiscountPrice())
                .sku(req.getSku())
                .imageUrl(req.getImageUrl())
                .category(category)
                .fabricType(req.getFabricType())
                .occasion(req.getOccasion())
                .isFeatured(Boolean.TRUE.equals(req.getIsFeatured()))
                .isBestseller(Boolean.TRUE.equals(req.getIsBestseller()))
                .isTrending(Boolean.TRUE.equals(req.getIsTrending()))
                .isActive(req.getIsActive() == null ? true : req.getIsActive())
                .build();
        Product saved = productRepository.save(p);
        return productService.getProductDetail(saved.getProductId());
    }

    @Transactional
    public ProductDetailResponse update(Long productId, ProductUpdateRequest req) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (req.getName() != null) p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getPrice() != null) p.setPrice(req.getPrice());
        if (req.getDiscountPrice() != null) p.setDiscountPrice(req.getDiscountPrice());
        if (req.getSku() != null) p.setSku(req.getSku());
        if (req.getImageUrl() != null) p.setImageUrl(req.getImageUrl());
        if (req.getFabricType() != null) p.setFabricType(req.getFabricType());
        if (req.getOccasion() != null) p.setOccasion(req.getOccasion());
        if (req.getIsFeatured() != null) p.setIsFeatured(req.getIsFeatured());
        if (req.getIsBestseller() != null) p.setIsBestseller(req.getIsBestseller());
        if (req.getIsTrending() != null) p.setIsTrending(req.getIsTrending());
        if (req.getIsActive() != null) p.setIsActive(req.getIsActive());
        if (req.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            p.setCategory(category);
        }

        productRepository.save(p);
        return productService.getProductDetail(p.getProductId());
    }

    /** Soft delete: flips isActive=false so order history references stay intact. */
    @Transactional
    public void softDelete(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        p.setIsActive(false);
        productRepository.save(p);
    }

    /** Uploads a file from the admin's computer and sets it as the product's hero image. */
    @Transactional
    public ProductDetailResponse uploadHeroImage(Long productId, MultipartFile file) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        String url = imageStorageService.store(file, "products/" + productId);
        p.setImageUrl(url);
        productRepository.save(p);
        return productService.getProductDetailForAdmin(productId);
    }

    /* ---------- Variants ---------- */

    @Transactional
    public ProductDetailResponse addVariant(Long productId, VariantCreateRequest req) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductVariant v = ProductVariant.builder()
                .product(p)
                .color(req.getColor())
                .size(req.getSize())
                .stockQuantity(req.getStockQuantity())
                .skuVariant(req.getSkuVariant())
                .build();
        variantRepository.save(v);
        return productService.getProductDetail(productId);
    }

    @Transactional
    public ProductDetailResponse updateVariant(Long productId, Long variantId, VariantUpdateRequest req) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!v.getProduct().getProductId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to this product");
        }
        if (req.getColor() != null) v.setColor(req.getColor());
        if (req.getSize() != null) v.setSize(req.getSize());
        if (req.getStockQuantity() != null) v.setStockQuantity(req.getStockQuantity());
        if (req.getSkuVariant() != null) v.setSkuVariant(req.getSkuVariant());
        variantRepository.save(v);
        return productService.getProductDetail(productId);
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!v.getProduct().getProductId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to this product");
        }
        // Best-effort: also remove its images from disk so we don't leak files.
        imageRepository.findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(variantId)
                .forEach(img -> imageStorageService.delete(img.getImageUrl()));
        variantRepository.delete(v);
    }

    /* ---------- Images ---------- */

    @Transactional
    public ImageUploadResponse uploadVariantImage(Long productId, Long variantId,
                                                  MultipartFile file, String viewType,
                                                  Boolean isPrimary) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!v.getProduct().getProductId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to this product");
        }

        String url = imageStorageService.store(file, "products/" + productId);

        // If this becomes the primary, demote any existing primary on the same variant.
        if (Boolean.TRUE.equals(isPrimary)) {
            imageRepository.findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(variantId)
                    .forEach(existing -> {
                        if (Boolean.TRUE.equals(existing.getIsPrimary())) {
                            existing.setIsPrimary(false);
                            imageRepository.save(existing);
                        }
                    });
        }

        ProductVariantImage img = ProductVariantImage.builder()
                .variant(v)
                .imageUrl(url)
                .viewType(viewType == null || viewType.isBlank() ? "FRONT" : viewType)
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .build();
        ProductVariantImage saved = imageRepository.save(img);

        return ImageUploadResponse.builder()
                .imageId(saved.getImageId())
                .imageUrl(saved.getImageUrl())
                .viewType(saved.getViewType())
                .isPrimary(Boolean.TRUE.equals(saved.getIsPrimary()))
                .build();
    }

    /** Marks one image as the variant's primary, demoting the variant's current primary. */
    @Transactional
    public void setPrimaryImage(Long imageId) {
        ProductVariantImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        Long variantId = img.getVariant().getVariantId();
        imageRepository.findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(variantId)
                .forEach(existing -> {
                    boolean shouldBePrimary = existing.getImageId().equals(imageId);
                    if (!Boolean.valueOf(shouldBePrimary).equals(existing.getIsPrimary())) {
                        existing.setIsPrimary(shouldBePrimary);
                        imageRepository.save(existing);
                    }
                });
    }

    @Transactional
    public void deleteImage(Long imageId) {
        ProductVariantImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        imageStorageService.delete(img.getImageUrl());
        imageRepository.delete(img);
    }
}
