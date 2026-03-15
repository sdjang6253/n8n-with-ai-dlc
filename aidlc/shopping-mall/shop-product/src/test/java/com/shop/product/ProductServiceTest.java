package com.shop.product;

import com.shop.product.dto.ProductResponse;
import com.shop.product.entity.Category;
import com.shop.product.entity.Product;
import com.shop.product.exception.InsufficientStockException;
import com.shop.product.exception.ProductNotFoundException;
import com.shop.product.repository.CategoryRepository;
import com.shop.product.repository.ProductRepository;
import com.shop.product.service.ProductService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@JqwikSpringSupport
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category clothing;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        electronics = categoryRepository.save(Category.builder().name("전자기기").build());
        clothing    = categoryRepository.save(Category.builder().name("의류").build());
    }

    // ===== Unit Tests =====

    @Test
    void getProducts_returnsAll_whenNoFilter() {
        saveProduct("노트북", "설명", new BigDecimal("1000000"), 10, electronics);
        saveProduct("티셔츠", "설명", new BigDecimal("20000"), 5, clothing);

        Page<ProductResponse> result = productService.getProducts(0, 20, null, null);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void getProducts_filterByCategory() {
        saveProduct("노트북", "설명", new BigDecimal("1000000"), 10, electronics);
        saveProduct("티셔츠", "설명", new BigDecimal("20000"), 5, clothing);

        Page<ProductResponse> result = productService.getProducts(0, 20, "전자기기", null);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("전자기기");
    }

    @Test
    void getProducts_filterByKeyword() {
        saveProduct("삼성 노트북", "고성능 노트북", new BigDecimal("1500000"), 5, electronics);
        saveProduct("애플 맥북", "맥OS 노트북", new BigDecimal("2000000"), 3, electronics);
        saveProduct("면 티셔츠", "편안한 의류", new BigDecimal("15000"), 20, clothing);

        Page<ProductResponse> result = productService.getProducts(0, 20, null, "노트북");
        assertThat(result.getContent()).hasSize(2);
        result.getContent().forEach(p ->
            assertThat(p.getName() + " " + p.getDescription()).containsIgnoringCase("노트북")
        );
    }

    @Test
    void getProduct_notFound_throws404() {
        assertThatThrownBy(() -> productService.getProduct(99999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProduct_soldOut_flagIsTrue() {
        Product p = saveProduct("품절상품", "설명", new BigDecimal("10000"), 0, electronics);
        ProductResponse response = productService.getProduct(p.getId());
        assertThat(response.isSoldOut()).isTrue();
    }

    @Test
    void decreaseStock_success() {
        Product p = saveProduct("재고상품", "설명", new BigDecimal("10000"), 10, electronics);
        productService.decreaseStock(p.getId(), 3);

        ProductResponse updated = productService.getProduct(p.getId());
        assertThat(updated.getStock()).isEqualTo(7);
    }

    @Test
    void decreaseStock_insufficientStock_throws() {
        Product p = saveProduct("재고부족", "설명", new BigDecimal("10000"), 2, electronics);
        assertThatThrownBy(() -> productService.decreaseStock(p.getId(), 5))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void productResponse_hasAllRequiredFields() {
        Product p = saveProduct("테스트상품", "상품설명", new BigDecimal("50000"), 10, electronics);
        ProductResponse response = productService.getProduct(p.getId());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isNotBlank();
        assertThat(response.getPrice()).isNotNull();
        assertThat(response.getCategoryName()).isNotBlank();
        assertThat(response.getStock()).isGreaterThanOrEqualTo(0);
    }

    // ===== Property-Based Tests =====

    // Feature: shopping-mall, Property 6: 상품 목록 페이지네이션
    @Property(tries = 20)
    void property6_paginationReturnsAtMostPageSize(@ForAll @IntRange(min = 1, max = 10) int size) {
        productRepository.deleteAll();
        for (int i = 0; i < 15; i++) {
            saveProduct("상품" + i, "설명", new BigDecimal("10000"), 5, electronics);
        }

        Page<ProductResponse> result = productService.getProducts(0, size, null, null);
        assertThat(result.getContent().size()).isLessThanOrEqualTo(size);
    }

    // Feature: shopping-mall, Property 7: 카테고리 필터링 정확성
    @Property(tries = 10)
    void property7_categoryFilterReturnsOnlyMatchingProducts(
            @ForAll("categoryNames") String category) {
        productRepository.deleteAll();
        saveProduct("전자상품1", "설명", new BigDecimal("100000"), 5, electronics);
        saveProduct("전자상품2", "설명", new BigDecimal("200000"), 3, electronics);
        saveProduct("의류상품1", "설명", new BigDecimal("30000"), 10, clothing);

        Page<ProductResponse> result = productService.getProducts(0, 20, category, null);
        result.getContent().forEach(p ->
            assertThat(p.getCategoryName()).isEqualTo(category)
        );
    }

    // Feature: shopping-mall, Property 8: 키워드 검색 정확성
    @Property(tries = 10)
    void property8_keywordSearchReturnsMatchingProducts(
            @ForAll("searchKeywords") String keyword) {
        productRepository.deleteAll();
        saveProduct("삼성 " + keyword + " 제품", "설명", new BigDecimal("100000"), 5, electronics);
        saveProduct("LG 전자제품", "설명", new BigDecimal("80000"), 3, electronics);
        saveProduct("면 티셔츠", keyword + " 포함 설명", new BigDecimal("20000"), 10, clothing);

        Page<ProductResponse> result = productService.getProducts(0, 20, null, keyword);
        result.getContent().forEach(p -> {
            String combined = (p.getName() + " " + p.getDescription()).toLowerCase();
            assertThat(combined).contains(keyword.toLowerCase());
        });
    }

    // Feature: shopping-mall, Property 9: 상품 응답 필드 완전성
    @Property(tries = 20)
    void property9_productResponseHasAllFields(@ForAll @IntRange(min = 1, max = 100) int stock) {
        productRepository.deleteAll();
        Product p = saveProduct("필드테스트상품", "설명입니다",
                new BigDecimal("50000"), stock, electronics);

        ProductResponse response = productService.getProduct(p.getId());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isNotBlank();
        assertThat(response.getPrice()).isNotNull();
        assertThat(response.getCategoryName()).isNotBlank();
        assertThat(response.getStock()).isEqualTo(stock);
    }

    // ===== Providers =====

    @Provide
    Arbitrary<String> categoryNames() {
        return Arbitraries.of("전자기기", "의류");
    }

    @Provide
    Arbitrary<String> searchKeywords() {
        return Arbitraries.of("노트북", "스마트", "프리미엄", "특가");
    }

    // ===== Helper =====

    private Product saveProduct(String name, String description, BigDecimal price, int stock, Category category) {
        return productRepository.save(Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .category(category)
                .imageUrl("https://picsum.photos/200")
                .createdAt(LocalDateTime.now())
                .build());
    }
}
