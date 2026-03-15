-- ============================================================
-- 01-schema.sql
-- Shopping Mall 마이크로서비스 DB 스키마 초기화
-- ============================================================
SET NAMES utf8mb4;

-- ============================================================
-- shop_user DB
-- ============================================================
CREATE DATABASE IF NOT EXISTS shop_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_user;

CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- shop_product DB
-- ============================================================
CREATE DATABASE IF NOT EXISTS shop_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_product;

CREATE TABLE categories (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       DECIMAL(10, 2) NOT NULL,
    stock       INT NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    image_url   VARCHAR(500),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- ============================================================
-- shop_order DB
-- ============================================================
CREATE DATABASE IF NOT EXISTS shop_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_order;

CREATE TABLE carts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    UNIQUE KEY uq_cart_user_product (user_id, product_id)
);

CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT '주문완료',
    total_price DECIMAL(10, 2) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    quantity     INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============================================================
-- shop_review DB
-- ============================================================
CREATE DATABASE IF NOT EXISTS shop_review CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_review;

CREATE TABLE reviews (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id   BIGINT NOT NULL,
    rating     INT NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reviews_product_id (product_id)
);

-- ============================================================
-- shopuser 권한 부여 (Docker Compose / K8s 공통)
-- ============================================================
GRANT ALL PRIVILEGES ON shop_user.* TO 'shopuser'@'%';
GRANT ALL PRIVILEGES ON shop_product.* TO 'shopuser'@'%';
GRANT ALL PRIVILEGES ON shop_order.* TO 'shopuser'@'%';
GRANT ALL PRIVILEGES ON shop_review.* TO 'shopuser'@'%';
FLUSH PRIVILEGES;
