package com.shop.product.service;

import com.shop.product.dto.ProductResponse;
import com.shop.product.entity.Product;
import com.shop.product.exception.InsufficientStockException;
import com.shop.product.exception.ProductNotFoundException;
import com.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, int size, String category, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        boolean hasCategory = StringUtils.hasText(category);
        boolean hasKeyword = StringUtils.hasText(keyword);

        if (hasCategory && hasKeyword) {
            return productRepository
                    .findByCategoryNameAndNameContainingIgnoreCaseOrCategoryNameAndDescriptionContainingIgnoreCase(
                            category, keyword, category, keyword, pageable)
                    .map(ProductResponse::from);
        } else if (hasCategory) {
            return productRepository.findByCategoryName(category, pageable).map(ProductResponse::from);
        } else if (hasKeyword) {
            return productRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable)
                    .map(ProductResponse::from);
        } else {
            return productRepository.findAll(pageable).map(ProductResponse::from);
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(product);
    }

    @Transactional
    public void decreaseStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(id, quantity, product.getStock());
        }
        product.decreaseStock(quantity);
    }
}
