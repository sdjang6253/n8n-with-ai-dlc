package com.shop.product.repository;

import com.shop.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryName(String categoryName, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String desc, Pageable pageable);

    Page<Product> findByCategoryNameAndNameContainingIgnoreCaseOrCategoryNameAndDescriptionContainingIgnoreCase(
            String cat1, String name, String cat2, String desc, Pageable pageable);
}
