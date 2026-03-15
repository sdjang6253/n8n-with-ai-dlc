package com.shop.order.client;

import com.shop.order.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    public ProductDto getProduct(Long productId) {
        try {
            return restTemplate.getForObject(
                    productServiceUrl + "/products/" + productId,
                    ProductDto.class);
        } catch (RestClientException e) {
            log.error("Failed to get product {}: {}", productId, e.getMessage());
            throw new ServiceUnavailableException("Product service unavailable");
        }
    }

    public void decreaseStock(Long productId, int quantity) {
        try {
            Map<String, Integer> body = new HashMap<>();
            body.put("quantity", quantity);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(body);
            restTemplate.exchange(
                    productServiceUrl + "/products/" + productId + "/stock",
                    HttpMethod.PUT, entity, Void.class);
        } catch (RestClientException e) {
            log.error("Failed to decrease stock for product {}: {}", productId, e.getMessage());
            throw new ServiceUnavailableException("Product service unavailable");
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}
