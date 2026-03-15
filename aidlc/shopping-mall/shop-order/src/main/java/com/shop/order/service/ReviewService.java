package com.shop.order.service;

import com.shop.order.dto.ReviewRequest;
import com.shop.order.kafka.ReviewEvent;
import com.shop.order.kafka.ReviewEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final OrderService orderService;
    private final ReviewEventProducer reviewEventProducer;

    public void submitReview(Long userId, ReviewRequest request) {
        if (!orderService.hasPurchased(userId, request.getProductId())) {
            throw new ForbiddenException("구매 이력이 없습니다");
        }

        ReviewEvent event = ReviewEvent.builder()
                .userId(userId)
                .productId(request.getProductId())
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        reviewEventProducer.send(event);
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
