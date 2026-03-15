# AIDLC 실습 프로젝트

인프라 관리 학습용 마이크로서비스 쇼핑몰 프로젝트입니다.
현재는 Docker Compose로 로컬 실행하며, 추후 Kubernetes로 이전 예정입니다.

---

## 프로젝트 구조

```
aidlc/shopping-mall/
├── shop-frontend/       # React + Vite + nginx (port 13000)
├── shop-user/           # Spring Boot — 회원가입/로그인/JWT (port 18083)
├── shop-product/        # Spring Boot — 상품 목록/상세/재고 (port 18081)
├── shop-order/          # Spring Boot — 장바구니/주문/리뷰 Kafka Producer (port 18082)
├── shop-review/         # Spring Boot — 리뷰 Kafka Consumer/조회 (port 18084)
├── docker/              # Docker Compose 설정 (메인 실행 디렉토리)
│   ├── docker-compose.yml
│   ├── mysql/init/      # 스키마 + 시드 데이터
│   ├── prometheus/      # prometheus.yml + 알람 규칙
│   ├── loki/            # loki-config.yml + promtail-config.yml
│   ├── grafana/         # 대시보드 프로비저닝
│   └── alertmanager/    # alertmanager.yml (gitignore — 직접 작성 필요)
└── k6/                  # 부하 테스트 시나리오
    ├── lib/auth.js
    └── scenarios/
```

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| 프론트엔드 | React + Vite + nginx |
| 백엔드 | Spring Boot 3.x (Java 17) |
| DB | MySQL 8.0 |
| 메시징 | Apache Kafka (Zookeeper) |
| 모니터링 | Prometheus + Loki + Grafana + Promtail |
| 알람 라우팅 | AlertManager → Slack (`--profile alerting` 으로만 기동) |
| 컨테이너 | Docker Compose |
| 부하 테스트 | k6 |

---

## 서비스 구성 및 포트

| 서비스 | 호스트 포트 | 컨테이너 포트 | 설명 |
|---|---|---|---|
| shop-frontend | **13000** | 3000 | React 쇼핑몰 UI |
| shop-user | **18083** | 8083 | 회원가입 / 로그인 / JWT 발급 |
| shop-product | **18081** | 8081 | 상품 목록 / 상세 / 재고 관리 |
| shop-order | **18082** | 8082 | 장바구니 / 주문 / 리뷰 Kafka Producer |
| shop-review | **18084** | 8084 | 리뷰 Kafka Consumer / 조회 |
| MySQL | **13306** | 3306 | 4개 DB (shop_user, shop_product, shop_order, shop_review) |
| Zookeeper | **12181** | 2181 | Kafka 코디네이터 |
| Kafka | **19092** | 9092 | 메시지 브로커 (review-created 토픽) |
| Prometheus | **19090** | 9090 | 메트릭 수집 (15초 간격 scrape) |
| Loki | **13100** | 3100 | 로그 수집 |
| Grafana | **13001** | 13001 | 대시보드 (admin / admin) |
| AlertManager | **19093** | 9093 | 알람 라우팅 → Slack |

---

## 실행 방법

```bash
cd aidlc/shopping-mall/docker

# 기본 실행 (AlertManager 제외)
docker compose up --build -d

# AlertManager(Slack 알림) 포함 실행
docker compose --profile alerting up --build -d

# 종료
docker compose down
```

### 접속 URL

| 서비스 | URL |
|---|---|
| 쇼핑몰 | http://localhost:13000 |
| Grafana | http://localhost:13001 &nbsp;(admin / admin) |
| Prometheus | http://localhost:19090 |
| AlertManager | http://localhost:19093 &nbsp;(alerting 프로필 기동 시) |

---

## 테스트 계정

| 이메일 | 비밀번호 | 이름 |
|---|---|---|
| alice@example.com | password123 | 김지수 |
| bob@example.com | password123 | 이민준 |
| carol@example.com | password123 | 박서연 |
| dave@example.com | password123 | 최현우 |
| eve@example.com | password123 | 정유나 |

---

## API 엔드포인트

### shop-user (port 18083)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | /auth/register | 불필요 | 회원가입 |
| POST | /auth/login | 불필요 | 로그인, JWT 반환 |
| GET | /simulate/error | 불필요 | 500 에러 시뮬레이션 |
| GET | /simulate/slow | 불필요 | 지연 시뮬레이션 (3000ms+) |
| GET | /simulate/memory | 불필요 | 메모리 할당 시뮬레이션 |

### shop-product (port 18081)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | /products | 불필요 | 상품 목록 (page, size, category, keyword) |
| GET | /products/:id | 불필요 | 상품 상세 |
| PUT | /products/:id/stock | 내부 | 재고 차감 (Order 서비스 호출) |
| GET | /simulate/* | 불필요 | 시뮬레이션 (error, slow, memory) |

### shop-order (port 18082)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | /cart | JWT | 장바구니 조회 |
| POST | /cart/items | JWT | 장바구니 상품 추가 |
| PUT | /cart/items/:productId | JWT | 수량 변경 |
| DELETE | /cart/items/:productId | JWT | 상품 삭제 |
| POST | /orders | JWT | 주문 생성 |
| GET | /orders | JWT | 주문 내역 조회 |
| POST | /reviews | JWT | 리뷰 작성 (Kafka produce) |
| GET | /simulate/* | 불필요 | 시뮬레이션 (error, slow, memory) |

### shop-review (port 18084)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | /reviews?productId={id} | 불필요 | 상품 리뷰 목록 |
| GET | /simulate/* | 불필요 | 시뮬레이션 (error, slow, memory) |

---

## 알람 시뮬레이션

각 서비스의 `/simulate/*` 엔드포인트로 Prometheus 알람을 직접 트리거할 수 있습니다.

```bash
# 500 에러 → IstioHigh5xxErrorRate 알람
curl http://localhost:18081/simulate/error

# 3초+ 지연 → HighLatency 알람
curl http://localhost:18081/simulate/slow

# 100MB 메모리 할당 → HighMemoryUsage 알람
curl http://localhost:18081/simulate/memory
```

포트를 `18082`, `18083`, `18084`로 바꿔 다른 서비스에도 동일하게 사용 가능합니다.

### Prometheus 알람 규칙

| 알람 | 조건 | 지속 시간 |
|---|---|---|
| IstioHigh5xxErrorRate | 5xx 에러율 > 10% | 1분 |
| HighMemoryUsage | JVM 힙 사용률 > 85% | 2분 |
| HighLatency | HTTP p99 > 2000ms | 1분 |

---

## k6 부하 테스트

```bash
# 정상 플로우 (로그인 → 상품 조회 → 장바구니 → 주문 → 리뷰)
k6 run aidlc/shopping-mall/k6/scenarios/01-normal-flow.js

# 에러 스파이크 → IstioHigh5xxErrorRate 알람 재현
k6 run aidlc/shopping-mall/k6/scenarios/02-error-spike.js

# 메모리 부하 → HighMemoryUsage 알람 재현
k6 run -e SERVICE=http://localhost:18081 aidlc/shopping-mall/k6/scenarios/03-memory-load.js

# 레이턴시 스파이크 → HighLatency 알람 재현
k6 run aidlc/shopping-mall/k6/scenarios/04-latency-spike.js
```

---

## Grafana 대시보드

`Shopping Mall Overview` 대시보드가 자동 프로비저닝됩니다.

| 패널 | 데이터소스 | 설명 |
|---|---|---|
| JVM Heap Memory Usage % | Prometheus | 서비스별 힙 사용률 |
| HTTP Request Rate | Prometheus | 서비스별 초당 요청 수 |
| HTTP Response Time p99 | Prometheus | 서비스별 p99 응답시간 |
| 5xx Error Rate | Prometheus | 서비스별 5xx 에러율 |
| JVM G1 Eden Space Usage % | Prometheus | Minor GC 톱니 패턴 확인 |
| JVM G1 Old Gen Usage % | Prometheus | 메모리 누수 감지 |
| Service Logs | Loki | 전체 서비스 로그 |
