package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddCartItemRequest;
import com.ecommerce.cart.dto.CartItemDto;
import com.ecommerce.cart.dto.CartSummaryResponse;
import com.ecommerce.cart.dto.ProductInfoResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.ApiException;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public CartSummaryResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return buildSummary(cart, items);
    }

    @Transactional
    public CartSummaryResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        ProductInfoResponse product = productClient.getProduct(request.productId());

        if (product.available() == null || !product.available()) {
            throw new ApiException(409, "PRODUCT_UNAVAILABLE",
                    "Product " + request.productId() + " is not available");
        }
        if (request.quantity() > product.stockQuantity()) {
            throw new ApiException(409, "INSUFFICIENT_STOCK",
                    "Requested quantity " + request.quantity()
                            + " exceeds available stock " + product.stockQuantity());
        }

        List<CartItem> items = new ArrayList<>(cartItemRepository.findByCartId(cart.getId()));
        CartItem existing = items.stream()
                .filter(i -> i.getProductId().equals(request.productId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + request.quantity();
            if (newQty > product.stockQuantity()) {
                throw new ApiException(409, "INSUFFICIENT_STOCK",
                        "Total quantity " + newQty
                                + " exceeds available stock " + product.stockQuantity());
            }
            existing.setQuantity(newQty);
            existing.setUnitPrice(product.price());
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem(cart, request.productId(), request.quantity(), product.price());
            cartItemRepository.save(item);
            items.add(item);
        }

        return buildSummary(cart, items);
    }

    @Transactional
    public CartSummaryResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ApiException(404, "ITEM_NOT_FOUND",
                        "Cart item " + itemId + " not found"));

        ProductInfoResponse product = productClient.getProduct(item.getProductId());
        if (request.quantity() > product.stockQuantity()) {
            throw new ApiException(409, "INSUFFICIENT_STOCK",
                    "Requested quantity " + request.quantity()
                            + " exceeds available stock " + product.stockQuantity());
        }

        item.setQuantity(request.quantity());
        item.setUnitPrice(product.price());
        cartItemRepository.save(item);

        List<CartItem> items = new ArrayList<>(cartItemRepository.findByCartId(cart.getId()));
        return buildSummary(cart, items);
    }

    @Transactional
    public CartSummaryResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ApiException(404, "ITEM_NOT_FOUND",
                        "Cart item " + itemId + " not found"));

        cartItemRepository.delete(item);

        List<CartItem> items = new ArrayList<>(cartItemRepository.findByCartId(cart.getId()));
        return buildSummary(cart, items);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cartItemRepository.deleteAll(items);
    }

    @Transactional
    public CartSummaryResponse checkout(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApiException(400, "EMPTY_CART", "Cannot checkout an empty cart");
        }
        return buildSummary(cart, items);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    private CartSummaryResponse buildSummary(Cart cart, List<CartItem> items) {
        List<CartItemDto> dtos = items.stream()
                .map(this::toDto)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        return new CartSummaryResponse(cart.getId(), dtos, subtotal, tax, total);
    }

    private CartItemDto toDto(CartItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        String productName = resolveProductName(item.getProductId());
        return new CartItemDto(item.getId(), item.getProductId(), productName, item.getQuantity(),
                item.getUnitPrice(), lineTotal);
    }

    private String resolveProductName(Long productId) {
        try {
            ProductInfoResponse product = productClient.getProduct(productId);
            return product.name();
        } catch (Exception e) {
            log.warn("Could not resolve product name for {}", productId);
            return "Product #" + productId;
        }
    }
}
