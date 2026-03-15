package com.shop.order.repository;

import com.shop.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndItemsProductId(Long userId, Long productId);
}
