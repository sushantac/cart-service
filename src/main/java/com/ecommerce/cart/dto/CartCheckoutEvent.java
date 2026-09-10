package com.ecommerce.cart.dto;

import com.ecommerce.cart.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartCheckoutEvent(
        String eventId,
        Long cartId,
        Long userId,
        List<CartItemSnapshot> items,
        java.time.LocalDateTime occurredAt
) {

    public record CartItemSnapshot(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        public static CartItemSnapshot from(CartItem item) {
            return new CartItemSnapshot(item.getProductId(), item.getQuantity(), item.getUnitPrice());
        }
    }
}
