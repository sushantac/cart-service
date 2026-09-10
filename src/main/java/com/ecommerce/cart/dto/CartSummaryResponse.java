package com.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryResponse(
        Long id,
        List<CartItemDto> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total
) {
}
