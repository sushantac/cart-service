package com.ecommerce.cart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient productWebClient(
            @Value("${app.product-service.url}") String productServiceUrl,
            @Value("${app.internal.api-key}") String internalApiKey) {
        return WebClient.builder()
                .baseUrl(productServiceUrl)
                .defaultHeader("X-Internal-API-Key", internalApiKey)
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
