package com.shop.order.controller;

import com.shop.order.dto.ReviewRequest;
import com.shop.order.filter.JwtAuthFilter.UserPrincipal;
import com.shop.order.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Void> submitReview(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody ReviewRequest request) {
        reviewService.submitReview(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
