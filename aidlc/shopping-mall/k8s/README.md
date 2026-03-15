# Shopping Mall — Kubernetes 환경

로컬 Kubernetes (Docker Desktop K8s)에서 전체 서비스를 실행하는 환경입니다.
plain YAML 매니페스트로 구성되어 있으며, Helm 없이 `kubectl apply`로 배포합니다.

## 사전 요구사항

- Docker Desktop + Kubernetes 활성화
- kubectl CLI
- 로컬 Docker 이미지 빌드 가능 (Docker Desktop K8s는 로컬 이미지 공유)

## 빠른 시작

```bash
cd aidlc/shopping-mall/k8s
bash deploy.sh
```

Windows에서 bash의 kubectl context가 다를 경우, PowerShell에서 deploy.sh의 명령을 직접 실행합니다:

```powershell
cd aidlc\shopping-mall\k8s
kubectl apply -f base\namespace.yaml
kubectl apply -f base\secrets.yaml
kubectl create configmap mysql-init-scripts --from-file=01-schema.sql=..\docker\mysql\init\01-schema.sql --from-file=02-seed.sql=..\docker\mysql\init\02-seed.sql -n shopping-mall --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f monitoring\configmap-prometheus.yaml -f monitoring\configmap-grafana.yaml -f monitoring\configmap-loki.yaml
docker build -t shopping-mall-shop-user:latest ..\shop-user\
docker build -t shopping-mall-shop-product:latest ..\shop-product\
docker build -t shopping-mall-shop-order:latest ..\shop-order\
docker build -t shopping-mall-shop-review:latest ..\shop-review\
docker build -t shopping-mall-shop-frontend:latest ..\shop-frontend\
kubectl apply -f infra\mysql.yaml -f infra\zookeeper.yaml
kubectl wait --for=condition=ready pod -l app=mysql -n shopping-mall --timeout=120s
kubectl apply -f infra\kafka.yaml
kubectl wait --for=condition=ready pod -l app=kafka -n shopping-mall --timeout=120s
kubectl apply -f apps\shop-user.yaml -f apps\shop-product.yaml -f apps\shop-review.yaml -f apps\shop-order.yaml -f apps\shop-frontend.yaml
kubectl apply -f monitoring\prometheus.yaml -f monitoring\loki.yaml -f monitoring\grafana.yaml
```

`deploy.sh`가 수행하는 작업:
1. `shopping-mall` 네임스페이스 + Secret 생성
2. ConfigMap 생성 (MySQL init SQL, Prometheus, Grafana, Loki 설정)
3. Docker 이미지 5개 빌드 (shop-user, shop-product, shop-order, shop-review, shop-frontend)
4. 인프라 서비스 배포 (MySQL → Zookeeper → Kafka, 순서 보장)
5. 애플리케이션 서비스 배포 (shop-user, shop-product, shop-order, shop-review, shop-frontend)
6. 모니터링 서비스 배포 (Prometheus, Loki, Grafana)

## 접속 방법 (ClusterIP + cloudflared / port-forward)

모든 서비스가 ClusterIP로 구성되어 있습니다.
외부 접근은 cloudflared 터널 또는 `kubectl port-forward`를 사용합니다.

```bash
# 프론트엔드
kubectl port-forward svc/shop-frontend 13000:13000 -n shopping-mall

# 모니터링
kubectl port-forward svc/grafana 13001:13001 -n shopping-mall
kubectl port-forward svc/prometheus 19090:19090 -n shopping-mall

# 백엔드 API (필요 시)
kubectl port-forward svc/shop-user 18083:18083 -n shopping-mall
kubectl port-forward svc/shop-product 18081:18081 -n shopping-mall
kubectl port-forward svc/shop-order 18082:18082 -n shopping-mall
kubectl port-forward svc/shop-review 18084:18084 -n shopping-mall
```

| 서비스 | port-forward 후 URL |
|---|---|
| 쇼핑몰 프론트엔드 | http://localhost:13000 |
| Grafana | http://localhost:13001 (admin / admin) |
| Prometheus | http://localhost:19090 |

## 상태 확인

```bash
kubectl get pods -n shopping-mall
kubectl get svc -n shopping-mall
kubectl logs -f deployment/shop-user -n shopping-mall
```

## 전체 삭제

```bash
bash teardown.sh
# 또는
kubectl delete namespace shopping-mall
```

## 매니페스트 구조

```
k8s/
├── base/                                # 기본 리소스
│   ├── namespace.yaml                   # shopping-mall 네임스페이스
│   └── secrets.yaml                     # MySQL / JWT 시크릿
├── infra/                               # 인프라 서비스
│   ├── mysql.yaml                       # MySQL Deployment + Service + PVC
│   ├── zookeeper.yaml                   # Zookeeper Deployment + Service
│   └── kafka.yaml                       # Kafka Deployment + Service
├── apps/                                # 애플리케이션 서비스
│   ├── shop-user.yaml                   # shop-user Deployment + Service
│   ├── shop-product.yaml                # shop-product Deployment + Service
│   ├── shop-order.yaml                  # shop-order Deployment + Service
│   ├── shop-review.yaml                 # shop-review Deployment + Service
│   └── shop-frontend.yaml              # shop-frontend Deployment + Service (ClusterIP 13000)
├── monitoring/                          # 모니터링 서비스
│   ├── prometheus.yaml                  # Prometheus Deployment + Service (ClusterIP 19090)
│   ├── loki.yaml                        # Loki Deployment + Service
│   ├── grafana.yaml                     # Grafana Deployment + Service (ClusterIP 13001)
│   ├── configmap-prometheus.yaml        # Prometheus 설정 + 알람 규칙
│   ├── configmap-grafana.yaml           # Grafana 데이터소스
│   └── configmap-loki.yaml             # Loki 설정
├── deploy.sh                            # 배포 스크립트
├── teardown.sh                          # 삭제 스크립트
└── README.md
```

mysql-init-scripts ConfigMap은 `deploy.sh`에서 `kubectl create configmap --from-file`로 SQL 파일에서 직접 생성합니다.

## Docker Compose와의 차이점

| 항목 | Docker Compose | Kubernetes |
|---|---|---|
| 기동 순서 | depends_on + healthcheck | deploy.sh에서 kubectl wait |
| 이미지 | docker compose build | docker build + imagePullPolicy: Never |
| 외부 접근 | 포트 매핑 | ClusterIP + cloudflared / port-forward |
| 설정 파일 | 볼륨 마운트 | ConfigMap |
| 민감 정보 | .env / .gitignore | Secret |
| Promtail | Docker socket 마운트 | 미포함 (k8s 환경에서는 별도 DaemonSet 필요) |
| AlertManager | profiles로 선택적 기동 | 미포함 (필요 시 별도 매니페스트 추가) |

## K8s 환경 주의사항

- Kafka Pod에 `enableServiceLinks: false` 설정 필수. K8s Service discovery가 자동 주입하는 `KAFKA_PORT` 환경변수가 Confluent Kafka 이미지와 충돌합니다.
- MySQL init SQL(`01-schema.sql`)에 `shopuser`에 대한 GRANT 문이 포함되어 있어야 합니다. K8s Secret에는 `MYSQL_DATABASE`가 없으므로 자동 권한 부여가 되지 않습니다.