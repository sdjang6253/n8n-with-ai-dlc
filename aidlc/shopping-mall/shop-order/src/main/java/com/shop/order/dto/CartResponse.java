package com.shop.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;

    @Data
    @Builder
    public static class CartItemResponse {
        private Long productId;
        private String productName;
        private BigDecimal price;
        private int quantity;
        private int stock;
        private String imageUrl;
        private BigDecimal subtotal;
    }
}
