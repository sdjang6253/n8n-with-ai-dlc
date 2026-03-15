package com.shop.review.service;

import com.shop.review.dto.ReviewResponse;
import com.shop.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long productId, int page, int size) {
        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(page, size))
                .map(ReviewResponse::from);
    }
}
