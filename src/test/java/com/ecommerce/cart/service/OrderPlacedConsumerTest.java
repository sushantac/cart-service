package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.OrderPlacedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderPlacedConsumerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderPlacedConsumer consumer;

    @Test
    void clearsCartWhenOrderPlaced() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                "evt-1", 101L, "ORD-101", 42L, new BigDecimal("120.00"),
                List.of(new OrderPlacedEvent.OrderItem(1L, 2, new BigDecimal("60.00"), new BigDecimal("120.00"))),
                LocalDateTime.now());

        consumer.onOrderPlaced(event);

        verify(cartService).clearCart(42L);
    }

    @Test
    void ignoresMalformedEvent() {
        consumer.onOrderPlaced(new OrderPlacedEvent(null, null, null, null, null, List.of(), null));

        verify(cartService, never()).clearCart(org.mockito.ArgumentMatchers.anyLong());
    }
}