# Cloudflare Tunnel + K8s 외부 접근 설정 가이드

## 전체 흐름 (Ingress Controller 없이 Tunnel 직접 연결)

```
사용자 브라우저
    │
    ▼ n8n.sdjang.cloud
Cloudflare Edge (SSL 종료, DDoS 방어)
    │
    ▼ Cloudflare Tunnel (암호화된 아웃바운드 연결)
cloudflared Pod (K8s 내부)
    │
    ▼ 서비스별 직접 연결 (K8s 내부 DNS)
    ├─ n8n.sdjang.cloud   → n8n.n8n.svc.cluster.local:5678
    ├─ nginx.sdjang.cloud → nginx.nginx.svc.cluster.local:80
    └─ xxx.sdjang.cloud   → xxx.{ns}.svc.cluster.local:{port}
```

Ingress Controller 없이 Cloudflare Tunnel의 Public Hostname이 각 K8s Service로 직접 라우팅.

## 사전 준비

- [x] Gabia에서 도메인 구매 (sdjang.cloud)
- [x] Gabia 네임서버를 Cloudflare로 변경
- [ ] Docker Desktop K8s 활성화 확인

## Step 1: Cloudflare Tunnel 생성

1. https://one.dash.cloudflare.com 접속
2. 좌측 `Networks` → `Tunnels`
3. `Create a tunnel` 클릭
4. Connector: `Cloudflared` 선택
5. 터널 이름: `home-pc-tunnel`
6. 생성 후 나오는 **토큰**을 복사

## Step 2: 토큰을 Secret에 설정

`k8s/cloudflared/secret.yaml` 파일에서 `<YOUR_TUNNEL_TOKEN>`을 실제 토큰으로 교체

## Step 3: K8s 리소스 배포

```bash
# Namespace
kubectl apply -f k8s/cloudflared/namespace.yaml
kubectl apply -f k8s/n8n/namespace.yaml

# Secrets (토큰/비밀번호 수정 후)
kubectl apply -f k8s/cloudflared/secret.yaml
kubectl apply -f k8s/n8n/secret.yaml

# n8n
kubectl apply -f k8s/n8n/pvc.yaml
kubectl apply -f k8s/n8n/deployment.yaml
kubectl apply -f k8s/n8n/service.yaml

# cloudflared
kubectl apply -f k8s/cloudflared/deployment.yaml
```

## Step 4: Cloudflare Public Hostname 설정

Cloudflare Zero Trust 대시보드 → Tunnel → Public Hostname 탭에서 서비스별로 추가:

| Subdomain | Domain | Service Type | URL |
|-----------|--------|-------------|-----|
| n8n | sdjang.cloud | HTTP | `n8n.n8n.svc.cluster.local:5678` |
| nginx | sdjang.cloud | HTTP | `nginx.nginx.svc.cluster.local:80` |

각 항목의 Additional settings:
- No TLS Verify: ON

## 서비스 추가 시

1. K8s에 Deployment + Service 배포
2. Cloudflare Tunnel에 Public Hostname 추가 (subdomain → 해당 서비스의 K8s 내부 DNS)

Ingress Controller 불필요. Tunnel 하나로 모든 서비스 라우팅.

## 검증

```bash
# Pod 상태 확인
kubectl get pods -n cloudflared
kubectl get pods -n n8n

# cloudflared 로그 확인
kubectl logs -n cloudflared -l app=cloudflared --tail=10
```

브라우저에서 `https://n8n.sdjang.cloud` 접속 → n8n UI가 보이면 성공

## 트러블슈팅

### cloudflared Pod가 CrashLoopBackOff
- Secret의 토큰이 올바른지 확인
- `kubectl logs -n cloudflared -l app=cloudflared`로 에러 확인

### 외부에서 접속 안 됨
- Cloudflare Tunnel 상태가 `HEALTHY`인지 확인 (대시보드)
- Public Hostname의 Service URL 오타 확인 (`svc` vs `scv` 등)
- `kubectl get svc -n {namespace}`로 서비스 이름/포트 확인

### n8n Webhook이 안 됨
- n8n의 `WEBHOOK_URL` 환경변수가 `https://n8n.sdjang.cloud/`인지 확인
- Slack Event Subscription URL을 `https://n8n.sdjang.cloud/webhook/...`으로 설정
