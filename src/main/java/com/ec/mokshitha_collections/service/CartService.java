package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.cart.CartItemResponse;
import com.ec.mokshitha_collections.dto.cart.CartResponse;
import com.ec.mokshitha_collections.entity.*;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.OutOfStockException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

    private final UserCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        UserCart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null) {
            return CartResponse.builder()
                    .items(List.of()).itemCount(0).totalQuantity(0)
                    .subtotal(BigDecimal.ZERO).build();
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, Long variantId, int quantity) {
        UserCart cart = getOrCreateCart(userId);
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        Product product = variant.getProduct();
        if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("Product is not available");
        }

        CartItem existing = cartItemRepository
                .findByCartCartIdAndVariantVariantId(cart.getCartId(), variantId)
                .orElse(null);

        int desired = (existing == null ? 0 : existing.getQuantity()) + quantity;
        assertStock(variant, desired);

        if (existing == null) {
            CartItem item = CartItem.builder()
                    .cart(cart).variant(variant).quantity(quantity).build();
            cartItemRepository.save(item);
        } else {
            existing.setQuantity(desired);
            cartItemRepository.save(existing);
        }
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = loadOwned(userId, cartItemId);
        assertStock(item.getVariant(), quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        CartItem item = loadOwned(userId, cartItemId);
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    /**
     * Frontend-compatible toggle for the product card "Add to cart" button.
     * Picks the first in-stock variant for the product. If any line for any
     * of that product's variants is already in the cart, removes them all
     * and reports {added: false}; otherwise adds qty=1 of the chosen variant
     * and reports {added: true}.
     */
    @Transactional
    public ToggleResult toggleProduct(Long userId, Long productId) {
        UserCart cart = getOrCreateCart(userId);

        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        if (variants.isEmpty()) {
            throw new BadRequestException("Product has no purchasable variants");
        }

        List<CartItem> existingForProduct = cart.getItems().stream()
                .filter(ci -> Objects.equals(
                        ci.getVariant().getProduct().getProductId(), productId))
                .toList();

        if (!existingForProduct.isEmpty()) {
            cartItemRepository.deleteAll(existingForProduct);
            cart.getItems().removeAll(existingForProduct);
            return new ToggleResult(false, getCart(userId));
        }

        ProductVariant chosen = variants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                .findFirst()
                .orElseThrow(() -> new OutOfStockException("All variants are out of stock"));

        CartItem item = CartItem.builder().cart(cart).variant(chosen).quantity(1).build();
        cartItemRepository.save(item);
        return new ToggleResult(true, getCart(userId));
    }

    private UserCart getOrCreateCart(Long userId) {
        return cartRepository.findByUserUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return cartRepository.save(UserCart.builder().user(user).build());
        });
    }

    private CartItem loadOwned(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this cart item");
        }
        return item;
    }

    private static void assertStock(ProductVariant variant, int desired) {
        Integer stock = variant.getStockQuantity();
        if (stock == null || stock < desired) {
            throw new OutOfStockException("Only " + (stock == null ? 0 : stock)
                    + " left in stock for the selected variant");
        }
    }

    private CartResponse toResponse(UserCart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQty = 0;
        var items = new java.util.ArrayList<CartItemResponse>();
        for (CartItem ci : cart.getItems()) {
            ProductVariant v = ci.getVariant();
            Product p = v.getProduct();
            BigDecimal unitPrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            totalQty += ci.getQuantity();
            items.add(CartItemResponse.builder()
                    .cartItemId(ci.getCartItemId())
                    .productId(p.getProductId())
                    .productName(p.getName())
                    .imageUrl(variantImageUrl(v, p))
                    .variantId(v.getVariantId())
                    .color(v.getColor())
                    .size(v.getSize())
                    .unitPrice(unitPrice)
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .stockAvailable(v.getStockQuantity())
                    .build());
        }
        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(items)
                .itemCount(items.size())
                .totalQuantity(totalQty)
                .subtotal(subtotal)
                .build();
    }

    /**
     * The image to show for a cart line: the selected variant's primary image
     * (or its first image), falling back to the product hero when the variant
     * has no images of its own.
     */
    private String variantImageUrl(ProductVariant v, Product p) {
        return variantImageRepository
                .findByVariantVariantIdOrderByIsPrimaryDescImageIdAsc(v.getVariantId())
                .stream()
                .findFirst()
                .map(ProductVariantImage::getImageUrl)
                .orElseGet(p::getImageUrl);
    }

    public record ToggleResult(boolean added, CartResponse cart) {}
}
