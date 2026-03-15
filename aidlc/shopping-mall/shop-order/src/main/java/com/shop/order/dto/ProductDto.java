package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String category;
    private int stock;
    private String imageUrl;
}
