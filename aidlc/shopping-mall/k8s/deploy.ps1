# Shopping Mall K8s 배포 스크립트 (PowerShell)
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== Shopping Mall K8s 배포 ===" -ForegroundColor Cyan

# 1. 네임스페이스 + 시크릿
Write-Host "[1/6] 네임스페이스 / Secret 생성..." -ForegroundColor Yellow
kubectl apply -f "$ScriptDir\base\namespace.yaml"
kubectl apply -f "$ScriptDir\base\secrets.yaml"

# 2. ConfigMap 생성
Write-Host "[2/6] ConfigMap 생성..." -ForegroundColor Yellow
kubectl create configmap mysql-init-scripts `
  --from-file="01-schema.sql=$ScriptDir\..\docker\mysql\init\01-schema.sql" `
  --from-file="02-seed.sql=$ScriptDir\..\docker\mysql\init\02-seed.sql" `
  -n shopping-mall --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f "$ScriptDir\monitoring\configmap-prometheus.yaml"
kubectl apply -f "$ScriptDir\monitoring\configmap-grafana.yaml"
kubectl apply -f "$ScriptDir\monitoring\configmap-loki.yaml"

# 3. 이미지 빌드
Write-Host "[3/6] Docker 이미지 빌드..." -ForegroundColor Yellow
docker build -t shopping-mall-shop-user:latest "$ScriptDir\..\shop-user\"
docker build -t shopping-mall-shop-product:latest "$ScriptDir\..\shop-product\"
docker build -t shopping-mall-shop-order:latest "$ScriptDir\..\shop-order\"
docker build -t shopping-mall-shop-review:latest "$ScriptDir\..\shop-review\"
docker build -t shopping-mall-shop-frontend:latest "$ScriptDir\..\shop-frontend\"

# 4. 인프라 서비스 배포
Write-Host "[4/6] 인프라 서비스 배포 (MySQL, Zookeeper, Kafka)..." -ForegroundColor Yellow
kubectl apply -f "$ScriptDir\infra\mysql.yaml"
kubectl apply -f "$ScriptDir\infra\zookeeper.yaml"
Write-Host "MySQL Ready 대기 (최대 120초)..."
kubectl wait --for=condition=ready pod -l app=mysql -n shopping-mall --timeout=120s
kubectl apply -f "$ScriptDir\infra\kafka.yaml"
Write-Host "Kafka Ready 대기 (최대 120초)..."
kubectl wait --for=condition=ready pod -l app=kafka -n shopping-mall --timeout=120s

# 5. 애플리케이션 서비스 배포
Write-Host "[5/6] 애플리케이션 서비스 배포..." -ForegroundColor Yellow
kubectl apply -f "$ScriptDir\apps\shop-user.yaml"
kubectl apply -f "$ScriptDir\apps\shop-product.yaml"
kubectl apply -f "$ScriptDir\apps\shop-review.yaml"
kubectl apply -f "$ScriptDir\apps\shop-order.yaml"
kubectl apply -f "$ScriptDir\apps\shop-frontend.yaml"

# 6. 모니터링 서비스 배포
Write-Host "[6/6] 모니터링 서비스 배포..." -ForegroundColor Yellow
kubectl apply -f "$ScriptDir\monitoring\prometheus.yaml"
kubectl apply -f "$ScriptDir\monitoring\loki.yaml"
kubectl apply -f "$ScriptDir\monitoring\grafana.yaml"

Write-Host ""
Write-Host "=== 배포 완료 ===" -ForegroundColor Green
Write-Host "Pod 상태 확인: kubectl get pods -n shopping-mall"
Write-Host ""
Write-Host "모든 서비스가 ClusterIP로 구성되어 있습니다."
Write-Host "cloudflared 터널 또는 kubectl port-forward로 접근하세요."
Write-Host ""
Write-Host "port-forward 예시:"
Write-Host "  kubectl port-forward svc/shop-frontend 13000:13000 -n shopping-mall"
Write-Host "  kubectl port-forward svc/grafana 13001:13001 -n shopping-mall"
Write-Host "  kubectl port-forward svc/prometheus 19090:19090 -n shopping-mall"
