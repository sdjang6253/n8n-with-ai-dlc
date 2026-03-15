# n8n-with-ai-dlc

> 인프라 관리 학습을 목적으로 구성한 로컬 k8s 실습 환경입니다.
> Cloudflare Tunnel로 외부 도메인을 확보하고, n8n 워크플로우로 알람 자동화를 테스트합니다.
> 실제 서비스 트래픽은 직접 구현한 쇼핑몰 마이크로서비스(aidlc)로 생성합니다.

---

## 전체 구조

```
n8n-with-ai-dlc/
├── aidlc/                  # 실습용 마이크로서비스 쇼핑몰
│   └── shopping-mall/      # Docker Compose 기반 5개 서비스
├── docs/                   # 설정 가이드 문서
│   ├── cloudflare-tunnel-setup-guide.md
│   ├── slack-bot-setup-guide.md
│   └── n8n-alert-workflow.md
├── k8s/                    # Kubernetes 매니페스트 (추후 추가 예정)
└── .kiro/specs/            # 설계 문서 (요구사항 / 설계 / 태스크)
```

---

## 목적 및 흐름

```
Cloudflare Tunnel
      │
      ▼
  로컬 k8s 클러스터 (Docker Desktop)
      │
      ├── n8n          ← 알람 워크플로우 자동화
      ├── Grafana       ← 대시보드 / 알람 확인
      └── aidlc 쇼핑몰 ← 실제 트래픽 / 알람 시뮬레이션 소스
```

1. **Cloudflare Tunnel** — 로컬 k8s에 외부 도메인 연결 (`docs/cloudflare-tunnel-setup-guide.md`)
2. **n8n** — Prometheus AlertManager 웹훅을 받아 Slack 알림 전송 (`docs/n8n-alert-workflow.md`)
3. **aidlc 쇼핑몰** — 실제 서비스를 띄워 메트릭/로그 생성, `/simulate/*` 엔드포인트로 알람 트리거

---

## aidlc 쇼핑몰

인프라 테스트용으로 직접 구현한 마이크로서비스 쇼핑몰입니다.
현재는 Docker Compose로 로컬 실행하며, 추후 k8s로 이전 예정입니다.

### 서비스 구성

| 서비스 | 호스트 포트 | 역할 |
|---|---|---|
| shop-frontend | 13000 | React + Vite UI |
| shop-user | 18083 | 회원가입 / 로그인 / JWT |
| shop-product | 18081 | 상품 목록 / 상세 / 재고 |
| shop-order | 18082 | 장바구니 / 주문 / 리뷰 Kafka Producer |
| shop-review | 18084 | 리뷰 Kafka Consumer / 조회 |
| MySQL | 13306 | 서비스별 독립 DB |
| Kafka | 19092 | review-created 토픽 |
| Prometheus | 19090 | 메트릭 수집 |
| Loki | 13100 | 로그 수집 |
| Grafana | 13001 | 대시보드 (admin / admin) |

### 빠른 시작 — Docker Compose

```bash
cd aidlc/shopping-mall/docker
docker compose up --build -d

# AlertManager(Slack 알림) 포함
docker compose --profile alerting up --build -d
```

### 빠른 시작 — Kubernetes

```bash
cd aidlc/shopping-mall/k8s
bash deploy.sh

# port-forward로 접근
kubectl port-forward svc/shop-frontend 13000:13000 -n shopping-mall
kubectl port-forward svc/grafana 13001:13001 -n shopping-mall
```

### 접속 URL

| 서비스 | URL |
|---|---|
| 쇼핑몰 | http://localhost:13000 |
| Grafana | http://localhost:13001 (admin / admin) |
| Prometheus | http://localhost:19090 |

### 알람 시뮬레이션

```bash
# 5xx 에러 → IstioHigh5xxErrorRate 알람
curl http://localhost:18081/simulate/error

# 응답 지연 → HighLatency 알람
curl http://localhost:18081/simulate/slow

# 메모리 압박 → HighMemoryUsage 알람
curl http://localhost:18081/simulate/memory
```

포트를 `18082`, `18083`, `18084`로 바꿔 다른 서비스에도 동일하게 사용 가능합니다.

### k6 부하 테스트

```bash
# 정상 플로우
k6 run aidlc/shopping-mall/k6/scenarios/01-normal-flow.js

# 에러 스파이크 (IstioHigh5xxErrorRate 알람 재현)
k6 run aidlc/shopping-mall/k6/scenarios/02-error-spike.js

# 메모리 부하 (HighMemoryUsage 알람 재현)
k6 run -e SERVICE=http://localhost:18081 aidlc/shopping-mall/k6/scenarios/03-memory-load.js

# 레이턴시 스파이크 (HighLatency 알람 재현)
k6 run aidlc/shopping-mall/k6/scenarios/04-latency-spike.js
```

자세한 내용은 [aidlc/README.md](./aidlc/README.md) 참고.

---

## 문서

| 문서 | 설명 |
|---|---|
| [docs/cloudflare-tunnel-setup-guide.md](./docs/cloudflare-tunnel-setup-guide.md) | Cloudflare Tunnel 설치 및 도메인 연결 |
| [docs/slack-bot-setup-guide.md](./docs/slack-bot-setup-guide.md) | Slack 봇 생성 및 웹훅 설정 |
| [docs/n8n-alert-workflow.md](./docs/n8n-alert-workflow.md) | n8n 알람 워크플로우 구성 |
| [aidlc/README.md](./aidlc/README.md) | 쇼핑몰 서비스 상세 문서 |

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| 인프라 | Docker Desktop k8s, Cloudflare Tunnel |
| 워크플로우 자동화 | n8n |
| 알림 | Slack |
| 모니터링 | Prometheus + Loki + Grafana |
| 쇼핑몰 프론트 | React + Vite |
| 쇼핑몰 백엔드 | Spring Boot 3.x (Java 17) |
| 메시징 | Apache Kafka |
| DB | MySQL 8.0 |
| 부하 테스트 | k6 |

---

## 로드맵

- [x] Docker Compose 기반 쇼핑몰 구현
- [x] Prometheus + Loki + Grafana 모니터링 연동
- [x] 알람 시뮬레이션 엔드포인트 (`/simulate/*`)
- [x] k6 부하 테스트 시나리오
- [ ] Cloudflare Tunnel + k8s 이전
- [ ] n8n 알람 워크플로우 연동
- [ ] OpenTelemetry 분산 추적 추가
