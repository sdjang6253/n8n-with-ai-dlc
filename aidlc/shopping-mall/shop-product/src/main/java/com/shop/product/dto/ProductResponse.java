package com.shop.product.dto;

import com.shop.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String categoryName;
    private int stock;
    private String imageUrl;
    private String description;
    private boolean soldOut;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .soldOut(product.getStock() == 0)
                .build();
    }
}
