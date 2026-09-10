package com.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderPlacedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        List<OrderItem> items,
        LocalDateTime occurredAt) {

    public record OrderItem(Long productId, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
    }
}