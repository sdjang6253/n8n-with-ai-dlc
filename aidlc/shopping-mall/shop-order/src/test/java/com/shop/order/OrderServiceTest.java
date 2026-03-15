package com.shop.order;

import com.shop.order.client.ProductServiceClient;
import com.shop.order.dto.CartItemRequest;
import com.shop.order.dto.CartResponse;
import com.shop.order.dto.OrderResponse;
import com.shop.order.dto.ProductDto;
import com.shop.order.entity.Cart;
import com.shop.order.kafka.ReviewEvent;
import com.shop.order.repository.CartRepository;
import com.shop.order.repository.OrderRepository;
import com.shop.order.service.CartService;
import com.shop.order.service.OrderService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@JqwikSpringSupport
class OrderServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private KafkaTemplate<String, ReviewEvent> kafkaTemplate;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        reset(productServiceClient);
    }

    // ===== Unit Tests =====

    @Test
    void addCartItem_success() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("1000000"), 10);

        CartItemRequest req = new CartItemRequest();
        setField(req, "productId", PRODUCT_ID);
        setField(req, "quantity", 2);

        cartService.addItem(USER_ID, req);

        List<Cart> carts = cartRepository.findByUserId(USER_ID);
        assertThat(carts).hasSize(1);
        assertThat(carts.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addCartItem_duplicate_mergesQuantity() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("1000000"), 10);

        CartItemRequest req1 = makeCartRequest(PRODUCT_ID, 2);
        CartItemRequest req2 = makeCartRequest(PRODUCT_ID, 3);

        cartService.addItem(USER_ID, req1);
        cartService.addItem(USER_ID, req2);

        List<Cart> carts = cartRepository.findByUserId(USER_ID);
        assertThat(carts).hasSize(1);
        assertThat(carts.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addCartItem_exceedsStock_throws() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("1000000"), 3);

        CartItemRequest req = makeCartRequest(PRODUCT_ID, 5);
        assertThatThrownBy(() -> cartService.addItem(USER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고");
    }

    @Test
    void getCart_returnsCorrectTotal() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("100000"), 10);
        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(3).build());

        CartResponse cart = cartService.getCart(USER_ID);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("300000"));
    }

    @Test
    void removeCartItem_success() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("100000"), 10);
        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(1).build());

        cartService.removeItem(USER_ID, PRODUCT_ID);

        assertThat(cartRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void createOrder_success_clearsCart() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("100000"), 10);
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());
        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(2).build());

        OrderResponse order = orderService.createOrder(USER_ID);

        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("주문완료");
        assertThat(order.getTotalPrice()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(cartRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void createOrder_emptyCart_throws() {
        assertThatThrownBy(() -> orderService.createOrder(USER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getOrders_returnsSortedByCreatedAtDesc() {
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());

        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(1).build());
        orderService.createOrder(USER_ID);
        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(1).build());
        orderService.createOrder(USER_ID);

        List<OrderResponse> orders = orderService.getOrders(USER_ID);
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getCreatedAt())
                .isAfterOrEqualTo(orders.get(1).getCreatedAt());
    }

    @Test
    void orderResponse_hasAllRequiredFields() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("100000"), 10);
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());
        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(1).build());

        OrderResponse order = orderService.createOrder(USER_ID);

        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getStatus()).isNotBlank();
        assertThat(order.getTotalPrice()).isNotNull();
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getItems()).isNotEmpty();
    }

    // ===== Property-Based Tests =====

    // Feature: shopping-mall, Property 10: 장바구니 추가 라운드 트립
    @Property(tries = 20)
    void property10_cartAddRoundTrip(@ForAll @IntRange(min = 1, max = 5) int quantity) {
        cartRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);

        CartItemRequest req = makeCartRequest(PRODUCT_ID, quantity);
        cartService.addItem(USER_ID, req);

        CartResponse cart = cartService.getCart(USER_ID);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(quantity);

        BigDecimal expectedTotal = new BigDecimal("10000").multiply(BigDecimal.valueOf(quantity));
        assertThat(cart.getTotalPrice()).isEqualByComparingTo(expectedTotal);
    }

    // Feature: shopping-mall, Property 11: 장바구니 수량 변경 반영
    @Property(tries = 20)
    void property11_cartUpdateReflected(
            @ForAll @IntRange(min = 1, max = 3) int initial,
            @ForAll @IntRange(min = 1, max = 5) int updated) {
        cartRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);

        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(initial).build());

        CartItemRequest updateReq = makeCartRequest(PRODUCT_ID, updated);
        cartService.updateItem(USER_ID, PRODUCT_ID, updateReq);

        CartResponse cart = cartService.getCart(USER_ID);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(updated);
    }

    // Feature: shopping-mall, Property 12: 장바구니 삭제 후 미포함
    @Property(tries = 20)
    void property12_cartRemoveNotIncluded(@ForAll @IntRange(min = 1, max = 5) int quantity) {
        cartRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);

        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(quantity).build());
        cartService.removeItem(USER_ID, PRODUCT_ID);

        CartResponse cart = cartService.getCart(USER_ID);
        assertThat(cart.getItems()).isEmpty();
    }

    // Feature: shopping-mall, Property 13: 주문 생성 상태 및 응답 필드
    @Property(tries = 10)
    void property13_orderCreationFields(@ForAll @IntRange(min = 1, max = 3) int quantity) {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());

        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(quantity).build());
        OrderResponse order = orderService.createOrder(USER_ID);

        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("주문완료");
        assertThat(order.getTotalPrice()).isNotNull();
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getItems()).isNotEmpty();
    }

    // Feature: shopping-mall, Property 15: 주문 완료 후 장바구니 비우기
    @Property(tries = 10)
    void property15_cartClearedAfterOrder(@ForAll @IntRange(min = 1, max = 3) int quantity) {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());

        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(quantity).build());
        orderService.createOrder(USER_ID);

        assertThat(cartRepository.findByUserId(USER_ID)).isEmpty();
    }

    // Feature: shopping-mall, Property 16: 주문 내역 최신순 정렬 및 필드 완전성
    @Property(tries = 10)
    void property16_ordersLatestFirst(@ForAll @IntRange(min = 2, max = 4) int orderCount) {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());

        for (int i = 0; i < orderCount; i++) {
            cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(1).build());
            orderService.createOrder(USER_ID);
        }

        List<OrderResponse> orders = orderService.getOrders(USER_ID);
        assertThat(orders).hasSize(orderCount);

        for (int i = 0; i < orders.size() - 1; i++) {
            assertThat(orders.get(i).getCreatedAt())
                    .isAfterOrEqualTo(orders.get(i + 1).getCreatedAt());
        }

        orders.forEach(o -> {
            assertThat(o.getOrderId()).isNotNull();
            assertThat(o.getStatus()).isNotBlank();
            assertThat(o.getTotalPrice()).isNotNull();
            assertThat(o.getCreatedAt()).isNotNull();
            assertThat(o.getItems()).isNotEmpty();
        });
    }

    // Feature: shopping-mall, Property 22: 장바구니 조회 시 최신 상품 정보 반영
    @Property(tries = 10)
    void property22_cartReflectsLatestProductInfo(@ForAll @IntRange(min = 1, max = 5) int quantity) {
        cartRepository.deleteAll();
        BigDecimal updatedPrice = new BigDecimal("20000");
        int updatedStock = 8;

        // 장바구니에 상품 저장
        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(quantity).build());

        // ProductServiceClient가 최신 가격/재고 반환하도록 설정
        mockProduct(PRODUCT_ID, "상품", updatedPrice, updatedStock);

        CartResponse cart = cartService.getCart(USER_ID);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getPrice()).isEqualByComparingTo(updatedPrice);
        assertThat(cart.getItems().get(0).getStock()).isEqualTo(updatedStock);

        BigDecimal expectedTotal = updatedPrice.multiply(BigDecimal.valueOf(quantity));
        assertThat(cart.getTotalPrice()).isEqualByComparingTo(expectedTotal);
    }

    // Feature: shopping-mall, Property 14: 주문 후 재고 차감 검증
    @Property(tries = 10)
    void property14_stockDecreasedAfterOrder(@ForAll @IntRange(min = 1, max = 3) int quantity) {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        mockProduct(PRODUCT_ID, "상품", new BigDecimal("10000"), 10);

        // decreaseStock 호출 여부 및 파라미터 검증
        doNothing().when(productServiceClient).decreaseStock(anyLong(), anyInt());

        cartRepository.save(Cart.builder().userId(USER_ID).productId(PRODUCT_ID).quantity(quantity).build());
        orderService.createOrder(USER_ID);

        verify(productServiceClient, times(1)).decreaseStock(PRODUCT_ID, quantity);
    }

    // Feature: shopping-mall, Task 7.13: 에지 케이스
    @Test
    void task713_reviewWithoutPurchaseHistory_throws403() {
        // 구매 이력 없는 사용자가 리뷰 작성 시 ForbiddenException
        com.shop.order.dto.ReviewRequest req = new com.shop.order.dto.ReviewRequest();
        setField(req, "productId", PRODUCT_ID);
        setField(req, "orderId", 999L);
        setField(req, "rating", 5);
        setField(req, "content", "좋아요");

        com.shop.order.service.ReviewService reviewService =
                new com.shop.order.service.ReviewService(orderService, null);

        assertThatThrownBy(() -> reviewService.submitReview(USER_ID, req))
                .isInstanceOf(com.shop.order.service.ReviewService.ForbiddenException.class);
    }

    @Test
    void task713_cartExceedsStock_throws400() {
        mockProduct(PRODUCT_ID, "노트북", new BigDecimal("1000000"), 3);

        CartItemRequest req = makeCartRequest(PRODUCT_ID, 10);
        assertThatThrownBy(() -> cartService.addItem(USER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고");
    }

    // ===== Helpers =====

    private void mockProduct(Long id, String name, BigDecimal price, int stock) {
        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setName(name);
        dto.setPrice(price);
        dto.setStock(stock);
        dto.setImageUrl("https://picsum.photos/200");
        when(productServiceClient.getProduct(id)).thenReturn(dto);
    }

    private CartItemRequest makeCartRequest(Long productId, int quantity) {
        CartItemRequest req = new CartItemRequest();
        setField(req, "productId", productId);
        setField(req, "quantity", quantity);
        return req;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
