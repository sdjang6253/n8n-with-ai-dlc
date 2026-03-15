-- ============================================================
-- 02-seed.sql
-- Shopping Mall 초기 더미 데이터
-- ============================================================
SET NAMES utf8mb4;

-- ============================================================
-- shop_user: 더미 유저 5개
-- password: "password123" bcrypt hash
-- ============================================================
USE shop_user;

INSERT INTO users (email, password, name) VALUES
('alice@example.com',   '$2a$10$qW0JCZI4nRW6/z/K0mGTwuf6Qk4P9gsubTbrc2ncs6Lg2QMsk8DAe', '김지수'),
('bob@example.com',     '$2a$10$qW0JCZI4nRW6/z/K0mGTwuf6Qk4P9gsubTbrc2ncs6Lg2QMsk8DAe', '이민준'),
('carol@example.com',   '$2a$10$qW0JCZI4nRW6/z/K0mGTwuf6Qk4P9gsubTbrc2ncs6Lg2QMsk8DAe', '박서연'),
('dave@example.com',    '$2a$10$qW0JCZI4nRW6/z/K0mGTwuf6Qk4P9gsubTbrc2ncs6Lg2QMsk8DAe', '최현우'),
('eve@example.com',     '$2a$10$qW0JCZI4nRW6/z/K0mGTwuf6Qk4P9gsubTbrc2ncs6Lg2QMsk8DAe', '정유나');

-- ============================================================
-- shop_product: 카테고리 4개 + 상품 20개
-- ============================================================
USE shop_product;

-- 카테고리
INSERT INTO categories (name) VALUES
('의류'),
('전자기기'),
('식품'),
('생활용품');

-- 의류 (category_id = 1)
INSERT INTO products (name, description, price, stock, category_id, image_url) VALUES
('무신사 스탠다드 오버핏 티셔츠', '루즈한 핏의 베이직 반팔 티셔츠. 순면 200g 중량감으로 늘어짐 없이 오래 입을 수 있습니다.', 29900.00, 150, 1, 'https://picsum.photos/seed/cloth1/400/400'),
('리바이스 511 슬림 청바지',       '클래식한 슬림 스트레이트 핏. 스트레치 데님 소재로 활동성이 뛰어납니다.',                  89900.00,  80, 1, 'https://picsum.photos/seed/cloth2/400/400'),
('나이키 에센셜 후드 집업',         '기모 안감으로 따뜻한 나이키 시그니처 후드 집업. 앞주머니 포함.',                          79900.00,  60, 1, 'https://picsum.photos/seed/cloth3/400/400'),
('자라 플로럴 미디 원피스',         '봄/여름 시즌 화사한 꽃무늬 패턴의 미디 기장 원피스.',                                    59900.00,  45, 1, 'https://picsum.photos/seed/cloth4/400/400'),
('코오롱 울 혼방 롱 코트',          '울 60% 혼방 소재의 클래식 더블 버튼 롱 코트. 가을/겨울 필수 아이템.',                   189900.00,  25, 1, 'https://picsum.photos/seed/cloth5/400/400');

-- 전자기기 (category_id = 2)
INSERT INTO products (name, description, price, stock, category_id, image_url) VALUES
('삼성 갤럭시 버즈3 프로',          '노이즈 캔슬링 + 360 오디오 지원. 최대 30시간 배터리. IPX7 방수.',                       219900.00,  55, 2, 'https://picsum.photos/seed/elec1/400/400'),
('애플 워치 SE 2세대',              '충돌 감지, 심박수 모니터링, GPS 내장. 알루미늄 케이스 40mm.',                           329000.00,  30, 2, 'https://picsum.photos/seed/elec2/400/400'),
('앤커 나노 65W GaN 충전기',        'GaN 기술 적용 초소형 65W 고속 충전기. USB-C 2포트 + USB-A 1포트.',                     39900.00, 120, 2, 'https://picsum.photos/seed/elec3/400/400'),
('JBL 플립 6 블루투스 스피커',      'IP67 방수/방진. 12시간 재생. 360도 사운드. 파티부스트 기능.',                           149000.00,  40, 2, 'https://picsum.photos/seed/elec4/400/400'),
('한성컴퓨터 TFG 기계식 키보드',    '청축 스위치 탑재 텐키리스 기계식 키보드. RGB 백라이트 + PBT 키캡.',                      69900.00,  35, 2, 'https://picsum.photos/seed/elec5/400/400');

-- 식품 (category_id = 3)
INSERT INTO products (name, description, price, stock, category_id, image_url) VALUES
('동원 참치 135g 12캔 세트',        '국내산 참치 원물 사용. 기름 담금 / 야채 담금 혼합 구성.',                                19900.00, 200, 3, 'https://picsum.photos/seed/food1/400/400'),
('스타벅스 콜드브루 블랙 500ml',    '스타벅스 원두를 20시간 저온 추출. 진하고 부드러운 콜드브루.',                            5900.00, 180, 3, 'https://picsum.photos/seed/food2/400/400'),
('올리타리아 엑스트라버진 올리브오일 500ml', '이탈리아 시칠리아산 콜드프레스 올리브오일. 산도 0.3% 이하.',                   22900.00,  90, 3, 'https://picsum.photos/seed/food3/400/400'),
('제주 삼다수 2L 12병',             '제주 화산암반수 100%. 미네랄 균형 잡힌 깨끗한 생수.',                                   13900.00, 300, 3, 'https://picsum.photos/seed/food4/400/400'),
('곰표 밀맥주 500ml 6캔',           '밀가루 브랜드 곰표와 협업한 밀맥주. 부드럽고 청량한 맛.',                                12900.00, 150, 3, 'https://picsum.photos/seed/food5/400/400');

-- 생활용품 (category_id = 4)
INSERT INTO products (name, description, price, stock, category_id, image_url) VALUES
('락앤락 클리어 밀폐용기 10종 세트', '전자레인지/냉동 사용 가능. 투명 뚜껑으로 내용물 확인 편리.',                            24900.00, 110, 4, 'https://picsum.photos/seed/life1/400/400'),
('무인양품 초음파 가습기 2.5L',      '조용한 초음파 방식. 연속 운전 최대 8시간. 아로마 트레이 포함.',                          59900.00,  50, 4, 'https://picsum.photos/seed/life2/400/400'),
('이케아 FRAKTA 대형 쇼핑백 5개',   '내구성 강한 폴리프로필렌 소재. 최대 25kg 적재 가능. 세탁 가능.',                          9900.00, 250, 4, 'https://picsum.photos/seed/life3/400/400'),
('스탠리 클래식 텀블러 473ml',       '18/8 스테인리스 스틸. 보온/보냉 24시간 유지. BPA 프리.',                                 49900.00,  75, 4, 'https://picsum.photos/seed/life4/400/400'),
('다이슨 에어랩 멀티 스타일러',      '열 없이 스타일링. 코안다 효과로 머리카락 손상 최소화. 6가지 어태치먼트 포함.',           699000.00,  15, 4, 'https://picsum.photos/seed/life5/400/400');

-- ============================================================
-- shop_order: 더미 주문 3개 + 주문 항목
-- ============================================================
USE shop_order;

-- 주문 1: user_id=1 (김지수) - 의류 + 전자기기
INSERT INTO orders (user_id, status, total_price, created_at) VALUES
(1, '주문완료', 249800.00, '2024-01-10 10:30:00');

INSERT INTO order_items (order_id, product_id, product_name, price, quantity) VALUES
(1, 1, '무신사 스탠다드 오버핏 티셔츠', 29900.00, 1),
(1, 6, '삼성 갤럭시 버즈3 프로',        219900.00, 1);

-- 주문 2: user_id=2 (이민준) - 식품 + 생활용품
INSERT INTO orders (user_id, status, total_price, created_at) VALUES
(2, '주문완료', 62700.00, '2024-01-15 14:20:00');

INSERT INTO order_items (order_id, product_id, product_name, price, quantity) VALUES
(2, 11, '동원 참치 135g 12캔 세트',  19900.00, 1),
(2, 12, '스타벅스 콜드브루 블랙 500ml', 5900.00, 3),
(2, 16, '락앤락 클리어 밀폐용기 10종 세트', 24900.00, 1);

-- 주문 3: user_id=1 (김지수) - 전자기기
INSERT INTO orders (user_id, status, total_price, created_at) VALUES
(1, '주문완료', 368900.00, '2024-01-20 09:15:00');

INSERT INTO order_items (order_id, product_id, product_name, price, quantity) VALUES
(3, 7, '애플 워치 SE 2세대',      329000.00, 1),
(3, 8, '앤커 나노 65W GaN 충전기', 39900.00, 1);

-- ============================================================
-- MySQL 권한 설정
-- ============================================================
GRANT ALL PRIVILEGES ON shop_user.*    TO 'shopuser'@'%';
GRANT ALL PRIVILEGES ON shop_product.* TO 'shopuser'@'%';
GRANT ALL PRIVILEGES ON shop_order.*   TO 'shopuser'@'%';
GRANT ALL PRIVILEGES ON shop_review.*  TO 'shopuser'@'%';
FLUSH PRIVILEGES;
