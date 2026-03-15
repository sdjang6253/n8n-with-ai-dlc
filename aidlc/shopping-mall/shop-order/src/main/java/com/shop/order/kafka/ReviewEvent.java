package com.shop.order.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewEvent {
    private Long userId;
    private Long productId;
    private Long orderId;
    private int rating;
    private String content;
    private LocalDateTime createdAt;
}
