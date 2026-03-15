package com.shop.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Long orderId;

    @Min(1) @Max(5)
    private int rating;

    @NotBlank
    @Size(max = 500)
    private String content;
}
