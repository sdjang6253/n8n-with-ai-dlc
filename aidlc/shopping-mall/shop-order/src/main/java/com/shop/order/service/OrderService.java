package com.shop.order.service;

import com.shop.order.client.ProductServiceClient;
import com.shop.order.dto.OrderResponse;
import com.shop.order.dto.ProductDto;
import com.shop.order.entity.Cart;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.repository.CartRepository;
import com.shop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public OrderResponse createOrder(Long userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있습니다");
        }

        Order order = Order.builder()
                .userId(userId)
                .status("주문완료")
                .totalPrice(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (Cart cart : carts) {
            ProductDto product = productServiceClient.getProduct(cart.getProductId());
            productServiceClient.decreaseStock(cart.getProductId(), cart.getQuantity());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(cart.getProductId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(cart.getQuantity())
                    .build();
            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        order.setTotalPrice(total);
        Order saved = orderRepository.save(order);
        cartRepository.deleteByUserId(userId);

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPurchased(Long userId, Long productId) {
        return orderRepository.existsByUserIdAndItemsProductId(userId, productId);
    }
}
