package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.ProductInfoResponse;
import com.ecommerce.cart.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductClientTest {

    private ProductClient buildClient(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        return new ProductClient(webClient);
    }

    @Test
    void getProduct_happyPath() {
        String json = """
                {"id":100,"name":"Widget","price":19.99,"stockQuantity":50,"available":true}
                """;

        ExchangeFunction exchange = request -> {
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(json)
                    .build();
            return Mono.just(response);
        };

        ProductClient client = buildClient(exchange);
        ProductInfoResponse product = client.getProduct(100L);

        assertNotNull(product);
        assertEquals(100L, product.id());
        assertEquals("Widget", product.name());
        assertEquals(new BigDecimal("19.99"), product.price());
        assertEquals(50, product.stockQuantity());
        assertEquals(true, product.available());
    }

    @Test
    void getProduct_notFound_throwsApiException() {
        ExchangeFunction exchange = request -> {
            ClientResponse response = ClientResponse.create(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/json")
                    .body("{}")
                    .build();
            return Mono.just(response);
        };

        ProductClient client = buildClient(exchange);

        ApiException ex = assertThrows(ApiException.class, () -> client.getProduct(999L));
        assertEquals(404, ex.getStatus());
        assertEquals("PRODUCT_NOT_FOUND", ex.getError());
    }
}
