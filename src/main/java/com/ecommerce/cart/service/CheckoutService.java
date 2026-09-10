package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartCheckoutEvent;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.ApiException;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final KafkaTemplate<String, CartCheckoutEvent> kafkaTemplate;
    private final String topicName;

    public CheckoutService(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           KafkaTemplate<String, CartCheckoutEvent> kafkaTemplate,
                           @org.springframework.beans.factory.annotation.Value("${app.kafka.topics.cart-checkout}") String topicName) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Transactional
    public void initiateCheckout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(400, "EMPTY_CART", "No cart found for user"));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApiException(400, "EMPTY_CART", "Cannot checkout an empty cart");
        }

        String eventId = UUID.randomUUID().toString();
        CartCheckoutEvent event = new CartCheckoutEvent(
                eventId,
                cart.getId(),
                userId,
                items.stream().map(CartCheckoutEvent.CartItemSnapshot::from).toList(),
                LocalDateTime.now()
        );

        kafkaTemplate.send(topicName, userId.toString(), event);
        log.info("Checkout initiated: eventId={}, cartId={}, userId={}", eventId, cart.getId(), userId);
    }
}
