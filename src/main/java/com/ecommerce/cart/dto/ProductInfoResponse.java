package com.ecommerce.cart.dto;

import java.math.BigDecimal;

public record ProductInfoResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stockQuantity,
        Boolean available
) {
}
