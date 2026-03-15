package com.shop.review.kafka;

import com.shop.review.entity.Review;
import com.shop.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final ReviewRepository reviewRepository;

    @KafkaListener(topics = "review-created", groupId = "shop-review-group")
    public void consume(ReviewEvent event) {
        log.info("Received review event: productId={}, userId={}", event.getProductId(), event.getUserId());
        Review review = Review.builder()
                .userId(event.getUserId())
                .productId(event.getProductId())
                .orderId(event.getOrderId())
                .rating(event.getRating())
                .content(event.getContent())
                .createdAt(event.getCreatedAt())
                .build();
        reviewRepository.save(review);
    }
}
