# Shopping Mall — Docker Compose 환경

Docker Compose로 전체 서비스를 로컬에서 실행하는 환경입니다.

## 사전 요구사항

- Docker Desktop (또는 Docker Engine + Docker Compose v2)
- 로컬에 Java / Maven / Node.js 불필요 (모두 컨테이너 내부에서 빌드)

## 빠른 시작

```bash
cd aidlc/shopping-mall/docker
docker compose up --build -d
```

> 첫 실행 시 Maven 의존성 다운로드로 5~10분 소요됩니다.

## 서비스 포트 매핑

| 서비스 | 호스트 포트 | 컨테이너 포트 |
|---|---|---|
| shop-frontend | 13000 | 3000 |
| shop-user | 18083 | 8083 |
| shop-product | 18081 | 8081 |
| shop-order | 18082 | 8082 |
| shop-review | 18084 | 8084 |
| MySQL | 13306 | 3306 |
| Zookeeper | 12190 | 2181 |
| Kafka | 19092 | 9092 |
| Prometheus | 19090 | 9090 |
| AlertManager | 19093 | 9093 |
| Loki | 13100 | 3100 |
| Grafana | 13001 | 3000 |

## 접속 URL

| 서비스 | URL |
|---|---|
| 쇼핑몰 프론트엔드 | http://localhost:13000 |
| 어드민 페이지 | http://localhost:13000/admin |
| Grafana | http://localhost:13001 (admin / admin) |
| Prometheus | http://localhost:19090 |
| AlertManager | http://localhost:19093 |

## AlertManager (Slack 알람) 설정

AlertManager는 `profiles: ["alerting"]`으로 기본 기동에서 제외됩니다.

### Slack 알람 활성화

```bash
# 1. 설정 파일 생성
cp alertmanager/alertmanager.yml.example alertmanager/alertmanager.yml

# 2. alertmanager.yml에서 Webhook URL과 채널명 수정
# slack_api_url: 'https://hooks.slack.com/services/실제URL'
# channel: '#원하는채널명'

# 3. AlertManager 포함 기동
docker compose --profile alerting up -d
```

> `alertmanager.yml`은 `.gitignore`에 등록되어 커밋되지 않습니다.

### AlertManager 없이 기동 (기본)

```bash
docker compose up --build -d
```

## 종료

```bash
docker compose down        # 컨테이너만 삭제 (데이터 유지)
docker compose down -v     # 볼륨까지 완전 삭제
```

## 로그 확인

```bash
docker compose logs -f              # 전체
docker compose logs -f shop-user    # 특정 서비스
```

## 디렉토리 구조

```
docker/
├── docker-compose.yml
├── alertmanager/
│   ├── alertmanager.yml           # .gitignore (실제 Webhook URL)
│   └── alertmanager.yml.example   # 템플릿
├── grafana/provisioning/
│   ├── dashboards/
│   │   ├── dashboards.yml
│   │   └── shopping-mall.json
│   └── datasources/
│       └── datasources.yml
├── loki/
│   ├── loki-config.yml
│   └── promtail-config.yml
├── mysql/init/
│   ├── 01-schema.sql
│   └── 02-seed.sql
└── prometheus/
    ├── prometheus.yml
    └── rules/alerts.yml
```

k6 시나리오는 `../k6/` 에 위치합니다. 실행 방법은 [aidlc/README.md](../../../README.md) 참고.
