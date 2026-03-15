# Requirements Document

## Introduction

인프라 관리 연습을 목적으로 하는 쇼핑몰 서비스입니다. React 프론트엔드와 Spring Boot 기반의 마이크로서비스 백엔드(상품/주문/사용자/리뷰)로 구성되며, Docker Compose 환경에서 개발 후 로컬 Kubernetes로 이전합니다. Prometheus + Loki + Grafana 모니터링 스택을 포함하며, n8n 알람 워크플로우 테스트를 위한 알람 시뮬레이션 엔드포인트를 제공합니다. 리뷰 기능은 Kafka를 통한 비동기 이벤트 기반으로 구현됩니다.

## Glossary

- **System**: 쇼핑몰 전체 시스템
- **User_Service**: 회원가입/로그인을 담당하는 Spring Boot 서비스 (port 18083)
- **Product_Service**: 상품 목록/상세/재고를 담당하는 Spring Boot 서비스 (port 18081)
- **Order_Service**: 장바구니/주문/주문내역을 담당하는 Spring Boot 서비스 (port 18082)
- **Frontend**: React 기반 웹 클라이언트 (port 13000)
- **JWT**: JSON Web Token, 인증 토큰
- **Prometheus**: 메트릭 수집 및 알람 서버 (port 19090)
- **Loki**: 로그 수집 서버 (port 13100)
- **Grafana**: 메트릭/로그 시각화 대시보드 (port 13001)
- **AlertManager**: Prometheus 알람 라우팅 컴포넌트
- **Actuator**: Spring Boot Actuator, 애플리케이션 메트릭 노출 엔드포인트
- **TraceId**: 분산 추적을 위한 요청 고유 식별자
- **Cart**: 사용자가 주문 전 상품을 담아두는 장바구니
- **Order**: 결제 시뮬레이션을 포함한 주문 완료 단위
- **Category**: 상품 분류 단위 (의류, 전자기기, 식품, 생활용품)
- **Review_Service**: 상품 리뷰 저장/조회를 담당하는 Spring Boot 서비스 (port 18084)
- **Review**: 구매 확인된 사용자가 상품에 작성하는 평점과 텍스트 후기
- **Kafka**: 분산 메시지 스트리밍 플랫폼, 서비스 간 비동기 이벤트 통신에 사용 (port 19092)
- **Kafka_Topic**: Kafka 메시지 채널 단위, `review-created` 토픽을 사용
- **Kafka_Producer**: Kafka 토픽에 이벤트를 발행하는 컴포넌트 (Order_Service가 담당)
- **Kafka_Consumer**: Kafka 토픽에서 이벤트를 구독하여 처리하는 컴포넌트 (Review_Service가 담당)
- **Zookeeper**: Kafka 클러스터 메타데이터 관리 컴포넌트 (port 12181)

---

## Requirements

### Requirement 1: 회원가입

**User Story:** As a 방문자, I want 이메일과 비밀번호로 회원가입을 할 수 있기를, so that 쇼핑몰 서비스를 이용할 수 있다.

#### Acceptance Criteria

1. WHEN 방문자가 이메일, 비밀번호, 이름을 제출하면, THE User_Service SHALL 이메일 중복 여부를 검증한다
2. WHEN 이메일이 중복되지 않고 입력값이 유효하면, THE User_Service SHALL 비밀번호를 bcrypt로 해시하여 사용자 정보를 저장한다
3. IF 이메일이 이미 존재하면, THEN THE User_Service SHALL HTTP 409 상태코드와 오류 메시지를 반환한다
4. IF 이메일 형식이 올바르지 않거나 비밀번호가 8자 미만이면, THEN THE User_Service SHALL HTTP 400 상태코드와 필드별 오류 메시지를 반환한다
5. THE System SHALL 더미 유저 계정 5개 이상을 초기 데이터로 포함한다

---

### Requirement 2: 로그인 및 JWT 인증

**User Story:** As a 회원, I want 이메일과 비밀번호로 로그인하여 JWT를 발급받기를, so that 인증이 필요한 기능을 사용할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 이메일과 비밀번호가 제출되면, THE User_Service SHALL 서명된 JWT Access Token을 반환한다
2. IF 이메일 또는 비밀번호가 일치하지 않으면, THEN THE User_Service SHALL HTTP 401 상태코드를 반환한다
3. THE User_Service SHALL JWT에 사용자 ID와 이메일을 클레임으로 포함한다
4. THE User_Service SHALL JWT 만료 시간을 24시간으로 설정한다
5. WHEN 인증이 필요한 API에 유효하지 않은 JWT가 전달되면, THE User_Service SHALL HTTP 401 상태코드를 반환한다
6. THE Frontend SHALL 발급된 JWT를 localStorage에 저장하고 이후 요청의 Authorization 헤더에 포함한다

---

### Requirement 3: 상품 목록 조회

**User Story:** As a 사용자, I want 상품 목록을 카테고리 필터와 키워드 검색으로 조회하기를, so that 원하는 상품을 쉽게 찾을 수 있다.

#### Acceptance Criteria

1. THE Product_Service SHALL 상품 목록을 페이지 단위(기본 20개)로 반환한다
2. WHEN 카테고리 파라미터가 전달되면, THE Product_Service SHALL 해당 카테고리의 상품만 필터링하여 반환한다
3. WHEN 검색 키워드가 전달되면, THE Product_Service SHALL 상품명 또는 설명에 키워드가 포함된 상품을 반환한다
4. THE Product_Service SHALL 각 상품에 상품 ID, 상품명, 가격, 카테고리, 재고 수량, 이미지 URL을 포함하여 반환한다
5. THE System SHALL 의류/전자기기/식품/생활용품 4개 카테고리에 걸쳐 20개 이상의 더미 상품 데이터를 초기 데이터로 포함한다
6. THE Product_Service SHALL 이미지 URL로 placeholder 이미지 서비스(예: picsum.photos)의 URL을 사용한다
7. THE System SHALL 더미 상품 데이터는 실제 한국 쇼핑몰 스타일의 상품명과 가격으로 구성한다

---

### Requirement 4: 상품 상세 조회

**User Story:** As a 사용자, I want 상품 상세 정보를 조회하기를, so that 구매 전 상품 정보를 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 상품 ID로 요청하면, THE Product_Service SHALL 상품명, 가격, 카테고리, 설명, 재고 수량, 이미지 URL을 반환한다
2. IF 존재하지 않는 상품 ID로 요청하면, THEN THE Product_Service SHALL HTTP 404 상태코드를 반환한다
3. WHILE 재고 수량이 0이면, THE Product_Service SHALL 해당 상품을 품절 상태로 표시한다

---

### Requirement 5: 장바구니

**User Story:** As a 로그인한 회원, I want 상품을 장바구니에 담고 수량을 조정하기를, so that 원하는 상품을 모아 한 번에 주문할 수 있다.

#### Acceptance Criteria

1. WHEN 로그인한 사용자가 상품과 수량을 전달하면, THE Order_Service SHALL 해당 상품을 사용자의 장바구니에 추가한다
2. WHEN 이미 장바구니에 있는 상품을 추가하면, THE Order_Service SHALL 기존 수량에 요청 수량을 합산한다
3. WHEN 사용자가 장바구니 상품의 수량을 변경하면, THE Order_Service SHALL 해당 항목의 수량을 업데이트한다
4. WHEN 사용자가 장바구니 상품을 삭제하면, THE Order_Service SHALL 해당 항목을 장바구니에서 제거한다
5. WHEN 로그인한 사용자가 장바구니를 조회하면, THE Order_Service SHALL 담긴 상품 목록, 각 수량, 합계 금액을 반환한다
6. IF 비로그인 사용자가 장바구니 API를 호출하면, THEN THE Order_Service SHALL HTTP 401 상태코드를 반환한다
7. IF 요청 수량이 재고 수량을 초과하면, THEN THE Order_Service SHALL HTTP 400 상태코드와 오류 메시지를 반환한다

---

### Requirement 6: 주문

**User Story:** As a 로그인한 회원, I want 장바구니 상품을 주문하기를, so that 상품 구매를 완료할 수 있다.

#### Acceptance Criteria

1. WHEN 로그인한 사용자가 주문을 요청하면, THE Order_Service SHALL 장바구니의 모든 항목으로 주문을 생성하고 상태를 "주문완료"로 설정한다
2. WHEN 주문이 생성되면, THE Order_Service SHALL Product_Service에 각 상품의 재고를 주문 수량만큼 차감 요청한다
3. WHEN 주문이 성공적으로 생성되면, THE Order_Service SHALL 주문 ID, 주문 항목, 총 금액, 주문 일시를 반환한다
4. WHEN 주문이 완료되면, THE Order_Service SHALL 사용자의 장바구니를 비운다
5. IF 주문 시점에 재고가 부족한 상품이 있으면, THEN THE Order_Service SHALL HTTP 409 상태코드와 재고 부족 상품 정보를 반환한다
6. THE Order_Service SHALL 결제를 시뮬레이션으로 처리하며 항상 결제 성공으로 응답한다
7. THE System SHALL 더미 주문 내역 데이터를 초기 데이터로 포함한다

---

### Requirement 7: 주문 내역 조회

**User Story:** As a 로그인한 회원, I want 나의 주문 내역을 조회하기를, so that 과거 구매 이력을 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 로그인한 사용자가 주문 내역을 요청하면, THE Order_Service SHALL 해당 사용자의 주문 목록을 최신순으로 반환한다
2. THE Order_Service SHALL 각 주문에 주문 ID, 주문 일시, 주문 상태, 총 금액, 주문 항목 목록을 포함하여 반환한다
3. IF 비로그인 사용자가 주문 내역 API를 호출하면, THEN THE Order_Service SHALL HTTP 401 상태코드를 반환한다

---

### Requirement 8: 모니터링 메트릭 수집

**User Story:** As a 인프라 운영자, I want Spring Boot 서비스의 메트릭을 Prometheus로 수집하기를, so that 서비스 상태를 모니터링할 수 있다.

#### Acceptance Criteria

1. THE User_Service, Product_Service, Order_Service, Review_Service SHALL Spring Actuator와 Micrometer를 통해 `/actuator/prometheus` 엔드포인트로 메트릭을 노출한다
2. THE Prometheus SHALL 각 Spring Boot 서비스의 `/actuator/prometheus` 엔드포인트를 15초 간격으로 스크래핑한다
3. THE System SHALL JVM 힙 메모리 사용량, HTTP 요청 수, HTTP 응답 시간, HTTP 상태코드별 카운트 메트릭을 수집한다
4. THE Grafana SHALL Prometheus를 데이터소스로 연결하여 수집된 메트릭을 시각화한다

---

### Requirement 9: 구조화 로그 수집

**User Story:** As a 인프라 운영자, I want Spring Boot 서비스의 로그를 Loki로 수집하기를, so that 로그를 중앙에서 검색하고 분석할 수 있다.

#### Acceptance Criteria

1. THE User_Service, Product_Service, Order_Service, Review_Service SHALL 모든 로그를 JSON 형식으로 출력한다
2. THE System SHALL 각 로그 항목에 timestamp, level, service명, traceId, message 필드를 포함한다
3. THE Loki SHALL 각 Spring Boot 서비스의 JSON 로그를 수집한다
4. THE Grafana SHALL Loki를 데이터소스로 연결하여 수집된 로그를 조회할 수 있다
5. WHEN 하나의 HTTP 요청이 여러 서비스를 거치면, THE System SHALL 동일한 traceId를 로그에 포함하여 요청 추적이 가능하도록 한다

---

### Requirement 10: 알람 시뮬레이션 엔드포인트

**User Story:** As a 인프라 운영자, I want 알람 시뮬레이션 엔드포인트를 통해 장애 상황을 재현하기를, so that Prometheus AlertManager → Slack → n8n 알람 워크플로우를 테스트할 수 있다.

#### Acceptance Criteria

1. THE Product_Service SHALL `/simulate/error` 엔드포인트를 제공하며, 호출 시 HTTP 500 응답을 반환한다
2. THE Product_Service SHALL `/simulate/slow` 엔드포인트를 제공하며, 호출 시 3000ms 이상 지연 후 응답한다
3. THE Product_Service SHALL `/simulate/memory` 엔드포인트를 제공하며, 호출 시 대용량 객체를 메모리에 할당하여 JVM 힙 사용량을 증가시킨다
4. THE User_Service SHALL `/simulate/error` 엔드포인트를 제공하며, 호출 시 HTTP 500 응답을 반환한다
5. THE User_Service SHALL `/simulate/slow` 엔드포인트를 제공하며, 호출 시 3000ms 이상 지연 후 응답한다
6. THE User_Service SHALL `/simulate/memory` 엔드포인트를 제공하며, 호출 시 대용량 객체를 메모리에 할당하여 JVM 힙 사용량을 증가시킨다
7. THE Order_Service SHALL `/simulate/error` 엔드포인트를 제공하며, 호출 시 HTTP 500 응답을 반환한다
8. THE Order_Service SHALL `/simulate/slow` 엔드포인트를 제공하며, 호출 시 3000ms 이상 지연 후 응답한다
9. THE Order_Service SHALL `/simulate/memory` 엔드포인트를 제공하며, 호출 시 대용량 객체를 메모리에 할당하여 JVM 힙 사용량을 증가시킨다
10. THE Review_Service SHALL `/simulate/error` 엔드포인트를 제공하며, 호출 시 HTTP 500 응답을 반환한다
11. THE Review_Service SHALL `/simulate/slow` 엔드포인트를 제공하며, 호출 시 3000ms 이상 지연 후 응답한다
12. THE Review_Service SHALL `/simulate/memory` 엔드포인트를 제공하며, 호출 시 대용량 객체를 메모리에 할당하여 JVM 힙 사용량을 증가시킨다
13. THE Prometheus SHALL User_Service, Product_Service, Order_Service, Review_Service 중 어느 서비스에서든 5xx 에러율이 10% 초과 시 IstioHigh5xxErrorRate 알람을 발생시키는 규칙을 포함한다
14. THE Prometheus SHALL User_Service, Product_Service, Order_Service, Review_Service 중 어느 서비스에서든 JVM 힙 사용률이 85% 초과 시 HighMemoryUsage 알람을 발생시키는 규칙을 포함한다
15. THE Prometheus SHALL User_Service, Product_Service, Order_Service, Review_Service 중 어느 서비스에서든 HTTP 응답 시간 p99가 2000ms 초과 시 HighLatency 알람을 발생시키는 규칙을 포함한다

---

### Requirement 11: Docker Compose 개발 환경

**User Story:** As a 개발자, I want Docker Compose로 전체 서비스를 로컬에서 실행하기를, so that 개발 및 테스트 환경을 쉽게 구성할 수 있다.

#### Acceptance Criteria

1. THE System SHALL 단일 `docker-compose.yml` 파일로 Frontend, User_Service, Product_Service, Order_Service, Review_Service, MySQL, Kafka, Zookeeper, Prometheus, Loki, Grafana를 모두 실행할 수 있어야 한다
2. THE System SHALL MySQL 초기화 시 스키마와 더미 데이터를 자동으로 적재한다
3. THE System SHALL 각 서비스가 지정된 포트(Frontend:13000, Product:18081, Order:18082, User:18083, Review:18084, MySQL:13306, Zookeeper:12181, Kafka:19092, Prometheus:19090, Loki:13100, Grafana:13001)로 접근 가능해야 한다
4. THE System SHALL Grafana에 Prometheus와 Loki 데이터소스가 자동으로 프로비저닝되어야 한다
5. WHEN `docker-compose up` 명령을 실행하면, THE System SHALL 서비스 간 의존성 순서(MySQL → Zookeeper → Kafka → Backend Services → Frontend)에 따라 기동되어야 한다

---

### Requirement 17: 프론트엔드 UI 테마

**User Story:** As a 사용자, I want 쇼핑몰 UI가 일관된 하늘색 테마로 구성되기를, so that 시각적으로 쾌적한 쇼핑 경험을 할 수 있다.

#### Acceptance Criteria

1. THE Frontend SHALL 주요 색상으로 하늘색(#0EA5E9 계열)을 사용한다
2. THE Frontend SHALL 네비게이션 바에 하늘색 배경과 흰색 텍스트를 적용한다
3. THE Frontend SHALL 상품 카드에 호버 효과와 그림자를 적용한다
4. THE Frontend SHALL 버튼 컴포넌트에 하늘색 배경과 흰색 텍스트를 적용한다
5. THE Frontend SHALL 전체 레이아웃에 일관된 폰트와 간격을 적용한다

---

### Requirement 18: 어드민 시뮬레이션 UI

**User Story:** As a 인프라 운영자, I want 웹 UI에서 시뮬레이션 엔드포인트를 클릭 한 번으로 트리거하기를, so that 알람 워크플로우를 빠르게 테스트할 수 있다.

#### Acceptance Criteria

1. THE Frontend SHALL `/admin` 경로에 어드민 페이지를 제공한다
2. THE Frontend SHALL 각 서비스(User, Product, Order, Review)별로 error/slow/memory 시뮬레이션 버튼을 제공한다
3. WHEN 버튼을 클릭하면, THE Frontend SHALL 해당 서비스의 `/simulate/{type}` 엔드포인트를 호출하고 응답 결과를 표시한다

**User Story:** As a 개발자, I want 마이크로서비스 간 REST HTTP 통신이 올바르게 동작하기를, so that 주문 처리 시 재고 차감 등 서비스 간 협력이 가능하다.

#### Acceptance Criteria

1. THE Order_Service SHALL 주문 생성 시 Product_Service의 재고 차감 API를 REST HTTP로 호출한다
2. THE Order_Service SHALL 장바구니 조회 시 Product_Service의 상품 정보 API를 REST HTTP로 호출하여 최신 가격과 재고를 반영한다
3. IF Product_Service가 응답하지 않으면, THEN THE Order_Service SHALL HTTP 503 상태코드를 반환한다
4. THE Frontend SHALL 각 백엔드 서비스(User:18083, Product:18081, Order:18082)에 직접 REST HTTP 요청을 전송한다

---

### Requirement 13: 리뷰 작성

**User Story:** As a 로그인한 회원, I want 구매한 상품에 리뷰를 작성하기를, so that 다른 사용자에게 구매 경험을 공유할 수 있다.

#### Acceptance Criteria

1. WHEN 로그인한 사용자가 상품 ID, 평점(1~5), 리뷰 내용을 제출하면, THE Order_Service SHALL 해당 사용자의 해당 상품 구매 이력을 확인한다
2. WHEN 구매 이력이 확인되면, THE Order_Service SHALL `review-created` Kafka 토픽에 사용자 ID, 상품 ID, 평점, 리뷰 내용, 작성 일시를 포함한 이벤트를 발행한다
3. WHEN Review_Service가 `review-created` 토픽에서 이벤트를 수신하면, THE Review_Service SHALL 리뷰 데이터를 저장한다
4. IF 해당 사용자가 해당 상품을 구매한 이력이 없으면, THEN THE Order_Service SHALL HTTP 403 상태코드와 오류 메시지를 반환한다
5. IF 평점이 1 미만이거나 5 초과이면, THEN THE Order_Service SHALL HTTP 400 상태코드와 오류 메시지를 반환한다
6. IF 리뷰 내용이 비어 있거나 500자를 초과하면, THEN THE Order_Service SHALL HTTP 400 상태코드와 오류 메시지를 반환한다
7. IF 비로그인 사용자가 리뷰 작성 API를 호출하면, THEN THE Order_Service SHALL HTTP 401 상태코드를 반환한다

---

### Requirement 14: 리뷰 조회

**User Story:** As a 사용자, I want 상품의 리뷰 목록을 조회하기를, so that 구매 전 다른 사용자의 경험을 참고할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 상품 ID로 리뷰 목록을 요청하면, THE Review_Service SHALL 해당 상품의 리뷰 목록을 최신순으로 반환한다
2. THE Review_Service SHALL 각 리뷰에 리뷰 ID, 사용자 ID, 평점, 리뷰 내용, 작성 일시를 포함하여 반환한다
3. THE Review_Service SHALL 리뷰 목록을 페이지 단위(기본 20개)로 반환한다
4. IF 존재하지 않는 상품 ID로 요청하면, THEN THE Review_Service SHALL 빈 목록과 HTTP 200 상태코드를 반환한다
5. THE Frontend SHALL 리뷰 조회 시 Review_Service(port 18084)에 직접 REST HTTP 요청을 전송한다

---

### Requirement 15: Kafka 인프라

**User Story:** As a 개발자, I want Kafka가 Docker Compose 환경에서 실행되기를, so that 서비스 간 비동기 이벤트 통신이 가능하다.

#### Acceptance Criteria

1. THE System SHALL Docker Compose에 Zookeeper(port 12181)와 Kafka(port 19092)를 포함한다
2. WHEN `docker-compose up` 명령을 실행하면, THE System SHALL Zookeeper가 기동된 후 Kafka가 기동되어야 한다
3. THE System SHALL Kafka 기동 시 `review-created` 토픽을 자동으로 생성한다
4. THE Order_Service SHALL Kafka Kafka_Producer로 동작하며 `review-created` 토픽에 이벤트를 발행한다
5. THE Review_Service SHALL Kafka Kafka_Consumer로 동작하며 `review-created` 토픽을 구독한다
6. IF Kafka가 일시적으로 응답하지 않으면, THEN THE Order_Service SHALL HTTP 503 상태코드를 반환한다
7. THE System SHALL Docker Compose의 서비스 기동 순서를 MySQL → Zookeeper → Kafka → Backend Services → Frontend 순으로 설정한다

---

### Requirement 16: Review_Service 모니터링 통합

**User Story:** As a 인프라 운영자, I want Review_Service의 메트릭과 로그를 기존 모니터링 스택에 통합하기를, so that 리뷰 서비스 상태를 동일한 대시보드에서 모니터링할 수 있다.

#### Acceptance Criteria

1. THE Review_Service SHALL Spring Actuator와 Micrometer를 통해 `/actuator/prometheus` 엔드포인트로 메트릭을 노출한다
2. THE Prometheus SHALL Review_Service의 `/actuator/prometheus` 엔드포인트를 15초 간격으로 스크래핑한다
3. THE Review_Service SHALL 모든 로그를 JSON 형식으로 출력하며 timestamp, level, service명, traceId, message 필드를 포함한다
4. THE Loki SHALL Review_Service의 JSON 로그를 수집한다
5. THE System SHALL Docker Compose의 서비스 포트 목록에 Review_Service(port 18084)를 포함한다
