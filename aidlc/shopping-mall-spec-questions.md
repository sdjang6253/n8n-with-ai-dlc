# 쇼핑몰 프로젝트 스펙 확인 질문지

## 확정된 사항

| 항목 | 내용 |
|------|------|
| Frontend | React |
| Backend | Spring Boot |
| DB | MySQL |
| 초기 개발 환경 | Docker Compose |
| 최종 배포 환경 | 로컬 k8s |
| 모니터링 | Prometheus + Loki (Docker → k8s 순서로 이전) |

---

## 1. 서비스 아키텍처

### 1-1. 백엔드 서비스 분리 방식
인프라 연습을 위해 백엔드를 여러 서비스로 나눌 예정입니다. 어떤 방식을 선호하시나요?

- [ ] A. `frontend` + `product-api` + `order-api`
  - 상품 조회/관리와 주문/결제를 분리
  - 서비스 간 HTTP 호출 발생 (order → product 재고 확인)
- [ ] B. `frontend` + `product-api` + `user-api`
  - 상품과 회원 서비스를 분리
  - 주문은 product-api에 포함
- [O] C. `frontend` + `product-api` + `order-api` + `user-api`
  - 3개 백엔드 서비스로 완전 분리
  - 서비스 간 호출이 많아져 Istio 메트릭 연습에 더 유리 (Istio 당장 고려 X)
- [ ] D. 기타 (직접 명시)

> 참고: 서비스 간 HTTP 호출이 많을수록 Istio 5xx 알람 시뮬레이션에 유리합니다.

---

## 2. 기능 범위

### 2-1. 사용자 기능
어느 수준까지 구현할까요?

- [ ] A. 최소 (로그인 없음)
  - 상품 목록 / 상세 페이지
  - 장바구니 (세션 기반)
  - 주문 (비회원 주문)
- [O] B. 기본
  - 위 + 회원가입 / 로그인 (JWT)
  - 주문 내역 조회
- [ ] C. 확장
  - 위 + 관리자 페이지 (상품 등록/수정/삭제, 주문 상태 변경)

### 2-2. 상품 기능 상세
- [O] 카테고리 분류 필요? (ex. 의류, 전자기기, 식품)
- [O] 상품 검색 기능 필요?
- [O] 상품 이미지는 실제 이미지 URL 사용? (ex. placeholder 이미지 서비스 활용)
- [O] 재고 수량 관리 필요? (주문 시 재고 차감)

### 2-3. 주문/결제
- [X] 실제 결제 연동 필요? (아니면 결제 완료 시뮬레이션만)
- [X] 주문 상태 관리 필요? (주문접수 → 결제완료 → 배송중 → 배송완료)
- 주문이 들어갔다는 것만 있으면 되고 주문완료만 있음
---

## 3. 더미 데이터

### 3-1. 상품 데이터 규모
- [O] 소규모: 상품 20~30개
- [ ] 중간: 상품 50~100개
- [ ] 대규모: 상품 200개 이상 (부하 테스트용)

### 3-2. 카테고리 구성 (카테고리 사용 시)
어떤 카테고리로 구성할까요?
- [O] A. 일반 쇼핑몰형: 의류, 전자기기, 식품, 생활용품
- [ ] B. 패션 특화: 상의, 하의, 신발, 액세서리
- [ ] C. 기타 (직접 명시)

### 3-3. 초기 유저 데이터
- [O] 더미 유저 계정 필요? (회원 기능 구현 시)
- [O] 더미 주문 내역 필요? (주문 내역 조회 테스트용)

---

## 4. 모니터링 연동

### 4-1. Prometheus 메트릭 수준
- [O] A. 기본 (Spring Actuator + Micrometer 기본 제공)
  - JVM 메트릭 (heap, GC, thread)
  - HTTP 요청 수 / 응답시간 / 상태코드
- [ ] B. 커스텀 비즈니스 메트릭 추가
  - 주문 생성 수 (counter)
  - 결제 실패율 (gauge)
  - 상품 조회 수 (counter)
- [ ] C. A + B 모두

### 4-2. Loki 로그 수준
- [ ] A. 기본 (stdout 로그 수집)
- [O] B. 구조화 로그 (JSON 포맷, traceId 포함)
  - Istio TraceID와 연동하여 알람 워크플로우 테스트에 활용 가능(일단은 istio 고민 X)

### 4-3. 알람 시뮬레이션용 엔드포인트
인프라 알람 테스트를 위해 의도적으로 에러를 발생시키는 엔드포인트가 필요한가요?
- [O] 5xx 에러 발생 엔드포인트 (IstioHigh5xxErrorRate 알람 테스트용)
- [O] 메모리 과다 사용 엔드포인트 (OOMKilled 알람 테스트용)
- [O] 응답 지연 엔드포인트 (latency 알람 테스트용)

---

## 5. 서비스 네이밍 및 포트

### 5-1. 서비스 네이밍 prefix
로컬 연습용 네이밍을 어떻게 할까요?

- [ ] A. `dev-shop-*` (ex. `dev-shop-frontend`, `dev-shop-product`, `dev-shop-order`)
- [ ] B. `local-mall-*` (ex. `local-mall-frontend`, `local-mall-product`)
- [O] C. 기타 (직접 명시) shop-frontend,shop-product,shop-order,shop-user

### 5-2. 로컬 포트 구성 (Docker Compose 기준)
기본 포트 구성 확인:

| 서비스 | 기본 포트 | 변경 필요? |
|--------|-----------|-----------|
| frontend | 3000 | |
| product-api | 8081 | |
| order-api | 8082 | |
| user-api | 8083 | |
| MySQL | 3306 | |
| Prometheus | 9090 | |
| Loki | 3100 | |
| Grafana | 3001 | |

---

## 6. 개발 우선순위

### 6-1. 단계별 개발 순서
어떤 순서로 진행할까요?

- [ ] A. 백엔드 API 먼저 → 프론트엔드 → 모니터링 연동
- [ ] B. Docker Compose 인프라 구성 먼저 → 백엔드 → 프론트엔드
- [ ] C. 서비스 하나씩 완성 (product-api 완성 → order-api → frontend)
- [O] D. 기타 - 너가 완성도가 높은 순으로 해줘

### 6-2. k8s 이전 시점
Docker Compose로 개발 완료 후 k8s로 이전할 때 어느 시점에 이전할까요?
- [O] A. 기능 개발 완료 후 한번에 이전
- [ ] B. 서비스 하나씩 순차적으로 이전하며 연습
- [ ] C. Docker Compose와 k8s 매니페스트를 동시에 작성

---

## 7. 기타

### 7-1. API Gateway / Reverse Proxy
서비스 앞단에 프록시가 필요한가요?
- [ ] Nginx (현재 k8s/nginx.yaml 있음)
- [ ] Traefik (현재 k8s/traefik 설정 있음)
- [O] 없음 (각 서비스 직접 접근)

### 7-2. 서비스 간 통신
백엔드 서비스 간 통신 방식은?
- [O] REST HTTP 호출 (단순, Istio 메트릭 잘 잡힘)
- [ ] 메시지 큐 (Kafka / RabbitMQ) - 복잡하지만 더 현실적

### 7-3. 추가로 포함하고 싶은 것
자유롭게 작성해주세요.
일단은 istio 를 고려하지 않고 작성을 해주고, k8s 로 넘어갈때 otel을 추가로 넣어서 otel + k8s instrumentation 으로 react 도 api 도 하나의 그라파나에서볼까 싶어 일단은 
---

> 위 질문에 답변해주시면 스펙 문서 작성 후 개발을 시작하겠습니다.
