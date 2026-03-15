#!/bin/bash
set -e

echo "=== Shopping Mall K8s 배포 ==="

# 1. 네임스페이스 + 시크릿
echo "[1/6] 네임스페이스 / Secret 생성..."
kubectl apply -f base/namespace.yaml
kubectl apply -f base/secrets.yaml

# 2. ConfigMap 생성
echo "[2/6] ConfigMap 생성..."
kubectl create configmap mysql-init-scripts \
  --from-file=01-schema.sql=../docker/mysql/init/01-schema.sql \
  --from-file=02-seed.sql=../docker/mysql/init/02-seed.sql \
  -n shopping-mall --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f monitoring/configmap-prometheus.yaml
kubectl apply -f monitoring/configmap-grafana.yaml
kubectl apply -f monitoring/configmap-loki.yaml

# 3. 이미지 빌드 (Docker Desktop K8s는 로컬 이미지 공유)
echo "[3/6] Docker 이미지 빌드..."
docker build -t shopping-mall-shop-user:latest ../shop-user/
docker build -t shopping-mall-shop-product:latest ../shop-product/
docker build -t shopping-mall-shop-order:latest ../shop-order/
docker build -t shopping-mall-shop-review:latest ../shop-review/
docker build -t shopping-mall-shop-frontend:latest ../shop-frontend/

# 4. 인프라 서비스 배포
echo "[4/6] 인프라 서비스 배포 (MySQL, Zookeeper, Kafka)..."
kubectl apply -f infra/mysql.yaml
kubectl apply -f infra/zookeeper.yaml
echo "MySQL Ready 대기 (최대 120초)..."
kubectl wait --for=condition=ready pod -l app=mysql -n shopping-mall --timeout=120s
kubectl apply -f infra/kafka.yaml
echo "Kafka Ready 대기 (최대 120초)..."
kubectl wait --for=condition=ready pod -l app=kafka -n shopping-mall --timeout=120s

# 5. 애플리케이션 서비스 배포
echo "[5/6] 애플리케이션 서비스 배포..."
kubectl apply -f apps/shop-user.yaml
kubectl apply -f apps/shop-product.yaml
kubectl apply -f apps/shop-review.yaml
kubectl apply -f apps/shop-order.yaml
kubectl apply -f apps/shop-frontend.yaml

# 6. 모니터링 서비스 배포
echo "[6/6] 모니터링 서비스 배포..."
kubectl apply -f monitoring/prometheus.yaml
kubectl apply -f monitoring/loki.yaml
kubectl apply -f monitoring/grafana.yaml

echo ""
echo "=== 배포 완료 ==="
echo "Pod 상태 확인: kubectl get pods -n shopping-mall"
echo ""
echo "모든 서비스가 ClusterIP로 구성되어 있습니다."
echo "cloudflared 터널 또는 kubectl port-forward로 접근하세요."
echo ""
echo "port-forward 예시:"
echo "  kubectl port-forward svc/shop-frontend 13000:13000 -n shopping-mall"
echo "  kubectl port-forward svc/grafana 13001:13001 -n shopping-mall"
echo "  kubectl port-forward svc/prometheus 19090:19090 -n shopping-mall"
