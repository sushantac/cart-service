package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final CartService cartService;

    public OrderPlacedConsumer(CartService cartService) {
        this.cartService = cartService;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-placed}", groupId = "cart-service")
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (event == null || event.eventId() == null || event.userId() == null) {
            log.warn("Ignoring malformed order.placed event");
            return;
        }
        cartService.clearCart(event.userId());
        log.info("Cart cleared after order placed: eventId={}, orderId={}, userId={}",
                event.eventId(), event.orderId(), event.userId());
    }
}