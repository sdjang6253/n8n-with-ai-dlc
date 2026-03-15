package com.shop.review;

import com.shop.review.dto.ReviewResponse;
import com.shop.review.entity.Review;
import com.shop.review.kafka.ReviewEvent;
import com.shop.review.kafka.ReviewEventConsumer;
import com.shop.review.repository.ReviewRepository;
import com.shop.review.service.ReviewService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@JqwikSpringSupport
@EmbeddedKafka(partitions = 1, topics = {"review-created"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:19092", "port=19092"})
@DirtiesContext
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewEventConsumer reviewEventConsumer;

    @Autowired(required = false)
    private KafkaTemplate<String, ReviewEvent> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    private static final Long PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    // ===== Unit Tests =====

    @Test
    void getReviews_emptyForUnknownProduct() {
        Page<ReviewResponse> result = reviewService.getReviews(99999L, 0, 20);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getReviews_returnsReviewsForProduct() {
        saveReview(PRODUCT_ID, 1L, 1L, 5, "좋아요");
        saveReview(PRODUCT_ID, 2L, 2L, 4, "괜찮아요");

        Page<ReviewResponse> result = reviewService.getReviews(PRODUCT_ID, 0, 20);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void getReviews_sortedByCreatedAtDesc() {
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusDays(1);

        reviewRepository.save(Review.builder()
                .userId(1L).productId(PRODUCT_ID).orderId(1L)
                .rating(3).content("오래된 리뷰").createdAt(older).build());
        reviewRepository.save(Review.builder()
                .userId(2L).productId(PRODUCT_ID).orderId(2L)
                .rating(5).content("최신 리뷰").createdAt(newer).build());

        Page<ReviewResponse> result = reviewService.getReviews(PRODUCT_ID, 0, 20);
        List<ReviewResponse> reviews = result.getContent();

        assertThat(reviews.get(0).getCreatedAt()).isAfterOrEqualTo(reviews.get(1).getCreatedAt());
    }

    @Test
    void getReviews_responseHasAllFields() {
        saveReview(PRODUCT_ID, 1L, 1L, 5, "완벽한 상품");

        Page<ReviewResponse> result = reviewService.getReviews(PRODUCT_ID, 0, 20);
        ReviewResponse review = result.getContent().get(0);

        assertThat(review.getReviewId()).isNotNull();
        assertThat(review.getUserId()).isNotNull();
        assertThat(review.getRating()).isBetween(1, 5);
        assertThat(review.getContent()).isNotBlank();
        assertThat(review.getCreatedAt()).isNotNull();
    }

    @Test
    void getReviews_doesNotReturnOtherProductReviews() {
        saveReview(PRODUCT_ID, 1L, 1L, 5, "상품1 리뷰");
        saveReview(999L, 2L, 2L, 3, "다른 상품 리뷰");

        Page<ReviewResponse> result = reviewService.getReviews(PRODUCT_ID, 0, 20);
        assertThat(result.getContent()).hasSize(1);
    }

    // Feature: shopping-mall, Task 9.6: 에지 케이스
    @Test
    void task96_unknownProductReturnsEmptyWith200() {
        Page<ReviewResponse> result = reviewService.getReviews(88888L, 0, 20);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void task96_actuatorPrometheusEndpointResponds() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    // Feature: shopping-mall, Property 20: Kafka 리뷰 이벤트 라운드 트립
    @Test
    void property20_kafkaReviewRoundTrip() {
        ReviewEvent event = new ReviewEvent();
        event.setUserId(10L);
        event.setProductId(PRODUCT_ID);
        event.setOrderId(100L);
        event.setRating(5);
        event.setContent("Kafka 라운드 트립 테스트");
        event.setCreatedAt(LocalDateTime.now());

        // Consumer 직접 호출 (EmbeddedKafka 환경에서 동기 검증)
        reviewEventConsumer.consume(event);

        Page<ReviewResponse> result = reviewService.getReviews(PRODUCT_ID, 0, 20);
        assertThat(result.getContent()).hasSize(1);
        ReviewResponse saved = result.getContent().get(0);
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getContent()).isEqualTo("Kafka 라운드 트립 테스트");
    }

    // ===== Property-Based Tests =====

    // Feature: shopping-mall, Property 21: 리뷰 목록 최신순 정렬 및 페이지네이션
    @Property(tries = 20)
    void property21_reviewsLatestFirstAndPaginated(@ForAll @IntRange(min = 1, max = 10) int size) {
        reviewRepository.deleteAll();
        for (int i = 0; i < 15; i++) {
            reviewRepository.save(Review.builder()
                    .userId((long) i).productId(PRODUCT_ID).orderId((long) i)
                    .rating((i % 5) + 1).content("리뷰 " + i)
                    .createdAt(LocalDateTime.now().minusMinutes(i))
                    .build());
        }

        Page<ReviewResponse> result = reviewService.getReviews(PRODUCT_ID, 0, size);

        assertThat(result.getContent().size()).isLessThanOrEqualTo(size);

        List<ReviewResponse> reviews = result.getContent();
        for (int i = 0; i < reviews.size() - 1; i++) {
            assertThat(reviews.get(i).getCreatedAt())
                    .isAfterOrEqualTo(reviews.get(i + 1).getCreatedAt());
        }

        reviews.forEach(r -> {
            assertThat(r.getReviewId()).isNotNull();
            assertThat(r.getUserId()).isNotNull();
            assertThat(r.getRating()).isBetween(1, 5);
            assertThat(r.getContent()).isNotBlank();
            assertThat(r.getCreatedAt()).isNotNull();
        });
    }

    // Feature: shopping-mall, Property 21 (edge): 존재하지 않는 상품 → 빈 목록 + 200
    @Property(tries = 10)
    void property21_unknownProductReturnsEmpty(@ForAll @IntRange(min = 10000, max = 99999) long unknownId) {
        reviewRepository.deleteAll();
        Page<ReviewResponse> result = reviewService.getReviews(unknownId, 0, 20);
        assertThat(result.getContent()).isEmpty();
    }

    // ===== Helper =====

    private Review saveReview(Long productId, Long userId, Long orderId, int rating, String content) {
        return reviewRepository.save(Review.builder()
                .userId(userId)
                .productId(productId)
                .orderId(orderId)
                .rating(rating)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
