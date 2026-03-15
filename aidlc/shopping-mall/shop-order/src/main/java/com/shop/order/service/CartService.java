package com.shop.order.service;

import com.shop.order.client.ProductServiceClient;
import com.shop.order.dto.CartItemRequest;
import com.shop.order.dto.CartResponse;
import com.shop.order.dto.ProductDto;
import com.shop.order.entity.Cart;
import com.shop.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);

        List<CartResponse.CartItemResponse> items = carts.stream().map(cart -> {
            ProductDto product = productServiceClient.getProduct(cart.getProductId());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            return CartResponse.CartItemResponse.builder()
                    .productId(cart.getProductId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(cart.getQuantity())
                    .stock(product.getStock())
                    .imageUrl(product.getImageUrl())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        BigDecimal total = items.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder().items(items).totalPrice(total).build();
    }

    @Transactional
    public void addItem(Long userId, CartItemRequest request) {
        ProductDto product = productServiceClient.getProduct(request.getProductId());
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("재고가 부족합니다");
        }

        cartRepository.findByUserIdAndProductId(userId, request.getProductId())
                .ifPresentOrElse(
                        cart -> {
                            int newQty = cart.getQuantity() + request.getQuantity();
                            if (product.getStock() < newQty) {
                                throw new IllegalArgumentException("재고가 부족합니다");
                            }
                            cart.setQuantity(newQty);
                        },
                        () -> cartRepository.save(Cart.builder()
                                .userId(userId)
                                .productId(request.getProductId())
                                .quantity(request.getQuantity())
                                .build())
                );
    }

    @Transactional
    public void updateItem(Long userId, Long productId, CartItemRequest request) {
        Cart cart = cartRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니에 없는 상품입니다"));
        ProductDto product = productServiceClient.getProduct(productId);
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("재고가 부족합니다");
        }
        cart.setQuantity(request.getQuantity());
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        cartRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }
}
