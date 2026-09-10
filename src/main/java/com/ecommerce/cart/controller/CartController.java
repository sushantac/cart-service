package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddCartItemRequest;
import com.ecommerce.cart.dto.CartSummaryResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.cart.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CartController(CartService cartService, CheckoutService checkoutService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    @GetMapping
    public CartSummaryResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(extractUserId(jwt));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartSummaryResponse addItem(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(extractUserId(jwt), request);
    }

    @PutMapping("/items/{itemId}")
    public CartSummaryResponse updateItem(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long itemId,
                                          @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(extractUserId(jwt), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartSummaryResponse removeItem(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long itemId) {
        return cartService.removeItem(extractUserId(jwt), itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(extractUserId(jwt));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void checkout(@AuthenticationPrincipal Jwt jwt) {
        checkoutService.initiateCheckout(extractUserId(jwt));
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("sub");
        if (claim instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(claim.toString());
    }
}
