package com.shop.order.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventProducer {

    private static final String TOPIC = "review-created";
    private final KafkaTemplate<String, ReviewEvent> kafkaTemplate;

    public void send(ReviewEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getProductId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send review event: {}", ex.getMessage());
                    } else {
                        log.info("Review event sent: productId={}", event.getProductId());
                    }
                });
    }
}
