package com.shop.review.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReviewEvent {
    private Long userId;
    private Long productId;
    private Long orderId;
    private int rating;
    private String content;
    private LocalDateTime createdAt;
}
