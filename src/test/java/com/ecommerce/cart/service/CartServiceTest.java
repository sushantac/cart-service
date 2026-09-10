package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddCartItemRequest;
import com.ecommerce.cart.dto.CartSummaryResponse;
import com.ecommerce.cart.dto.ProductInfoResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.ApiException;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long ITEM_ID = 10L;

    private Cart cart;
    private ProductInfoResponse product;

    @BeforeEach
    void setUp() {
        cart = new Cart(USER_ID);
        ReflectionTestUtils.setField(cart, "id", 100L);
        product = new ProductInfoResponse(PRODUCT_ID, "Test Product", new BigDecimal("29.99"), 50, true);
    }

    @Test
    void addItem_happyPath() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(new ArrayList<>());
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            ReflectionTestUtils.setField(item, "id", ITEM_ID);
            return item;
        });

        CartSummaryResponse response = cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 2));

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("59.98"), response.subtotal());
        assertEquals(new BigDecimal("4.80"), response.tax());
        assertEquals(new BigDecimal("64.78"), response.total());
    }

    @Test
    void addItem_insufficientStock_throwsApiException() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        ProductInfoResponse limitedProduct =
                new ProductInfoResponse(PRODUCT_ID, "Limited", new BigDecimal("10.00"), 3, true);
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(limitedProduct);

        ApiException ex = assertThrows(ApiException.class,
                () -> cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 5)));

        assertEquals(409, ex.getStatus());
        assertEquals("INSUFFICIENT_STOCK", ex.getError());
    }

    @Test
    void addItem_productUnavailable_throwsApiException() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        ProductInfoResponse unavailable =
                new ProductInfoResponse(PRODUCT_ID, "Unavailable", new BigDecimal("10.00"), 0, false);
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(unavailable);

        ApiException ex = assertThrows(ApiException.class,
                () -> cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 1)));

        assertEquals(409, ex.getStatus());
        assertEquals("PRODUCT_UNAVAILABLE", ex.getError());
    }

    @Test
    void addItem_existingItemIncreasesQuantity() {
        CartItem existing = new CartItem(cart, PRODUCT_ID, 2, new BigDecimal("29.99"));
        ReflectionTestUtils.setField(existing, "id", ITEM_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(new ArrayList<>(List.of(existing)));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartSummaryResponse response = cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 3));

        assertEquals(1, response.items().size());
        assertEquals(5, response.items().getFirst().quantity());
    }

    @Test
    void updateItem_happyPath() {
        CartItem existing = new CartItem(cart, PRODUCT_ID, 2, new BigDecimal("29.99"));
        ReflectionTestUtils.setField(existing, "id", ITEM_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(100L)).thenReturn(new ArrayList<>(List.of(existing)));

        CartSummaryResponse response = cartService.updateItem(USER_ID, ITEM_ID, new UpdateCartItemRequest(4));

        assertEquals(4, response.items().getFirst().quantity());
    }

    @Test
    void updateItem_insufficientStock_throwsApiException() {
        CartItem existing = new CartItem(cart, PRODUCT_ID, 2, new BigDecimal("29.99"));
        ReflectionTestUtils.setField(existing, "id", ITEM_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product);

        ApiException ex = assertThrows(ApiException.class,
                () -> cartService.updateItem(USER_ID, ITEM_ID, new UpdateCartItemRequest(100)));

        assertEquals(409, ex.getStatus());
        assertEquals("INSUFFICIENT_STOCK", ex.getError());
    }

    @Test
    void removeItem_happyPath() {
        CartItem existing = new CartItem(cart, PRODUCT_ID, 1, new BigDecimal("29.99"));
        ReflectionTestUtils.setField(existing, "id", ITEM_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartId(100L)).thenReturn(new ArrayList<>());

        CartSummaryResponse response = cartService.removeItem(USER_ID, ITEM_ID);

        assertNotNull(response);
        assertEquals(0, response.items().size());
        assertEquals(BigDecimal.ZERO, response.subtotal());
    }

    @Test
    void removeItem_notFound_throwsApiException() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> cartService.removeItem(USER_ID, ITEM_ID));
    }

    @Test
    void clearCart_happyPath() {
        CartItem item = new CartItem(cart, PRODUCT_ID, 3, new BigDecimal("29.99"));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(item));

        cartService.clearCart(USER_ID);

        verify(cartItemRepository).deleteAll(List.of(item));
    }

    @Test
    void totalsMath_includes8PercentTax() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(new ArrayList<>());
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            ReflectionTestUtils.setField(item, "id", ITEM_ID);
            return item;
        });

        CartSummaryResponse response = cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 3));

        BigDecimal expectedSubtotal = new BigDecimal("29.99").multiply(BigDecimal.valueOf(3));
        BigDecimal expectedTax = expectedSubtotal.multiply(new BigDecimal("0.08"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal expectedTotal = expectedSubtotal.add(expectedTax);

        assertEquals(expectedSubtotal, response.subtotal());
        assertEquals(expectedTax, response.tax());
        assertEquals(expectedTotal, response.total());
        assertTrue(response.total().compareTo(response.subtotal()) > 0);
    }

    @Test
    void getCart_emptyCart_returnsZeros() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(Collections.emptyList());

        CartSummaryResponse response = cartService.getCart(USER_ID);

        assertNotNull(response);
        assertTrue(response.items().isEmpty());
        assertTrue(response.subtotal().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(response.tax().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(response.total().compareTo(BigDecimal.ZERO) == 0);
    }
}
