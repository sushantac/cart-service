package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.ProductInfoResponse;
import com.ecommerce.cart.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private final WebClient webClient;

    public ProductClient(WebClient productWebClient) {
        this.webClient = productWebClient;
    }

    public ProductInfoResponse getProduct(Long productId) {
        log.debug("Fetching product {}", productId);
        return webClient.get()
                .uri("/api/v1/products/{id}", productId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Product not found")
                                .flatMap(body -> {
                                    if (resp.statusCode().value() == 404) {
                                        return Mono.error(new ApiException(404, "PRODUCT_NOT_FOUND",
                                                "Product " + productId + " not found"));
                                    }
                                    return Mono.error(new ApiException(resp.statusCode().value(),
                                            "PRODUCT_SERVICE_ERROR", body));
                                }))
                .bodyToMono(ProductInfoResponse.class)
                .block();
    }

    private static class Mono {
        static <T> reactor.core.publisher.Mono<T> error(Throwable ex) {
            return reactor.core.publisher.Mono.error(ex);
        }
    }
}
