# n8n Alert Investigation Workflow

## 개요

Slack 채널에서 Prometheus AlertManager 알람을 감지하고, AI Agent가 자동으로 이슈 원인을 분석하여 결과를 Slack에 다시 전송하는 n8n 워크플로우.

---

## 환경 정보

| 항목 | 내용 |
|------|------|
| n8n 환경 | 로컬 테스트 후 EKS self-hosted로 이전 예정 |
| Slack 연동 | Webhook 방식, Bot 신규 생성 필요 |
| 테스트 채널 | #general (이후 실제 알람 채널로 변경) |
| GitHub | PAT 사용, 레포는 알람 내용에서 동적으로 특정 |
| GitLab | Self-hosted, 서비스별 레포 + 관제 모노레포, Access Token 있음 |
| Loki | EKS 내부 FQDN, n8n도 EKS에 올라가므로 접근 가능 |
| Tempo | EKS 내부 엔드포인트, TraceID는 Loki 로그에서 추출 |
| Prometheus | 내부 접근 가능 |
| AI | LLM 사용하여 분석 요약 |

---

## 알람 메세지 포맷 (Prometheus AlertManager)

두 가지 주요 패턴:

### 패턴 1 - Istio/서비스 에러
```
[FIRING:1] IstioHigh5xxErrorRate .Alert: Istio high 5xx error rate - warning

Service: prd-mall-backoffice

Description: High percentage of HTTP 5xx responses in Istio (> 5%).
  VALUE = 28.57

Details:
  • alertname: IstioHigh5xxErrorRate
  • destination_app: prd-mall-backoffice
  • severity: warning
```

### 패턴 2 - Kubernetes 인프라 이벤트
```
[FIRING:1] KubernetesContainerOomKiller ...

Description: Container prd-mall-batch in pod prd-mall/prd-mall-batch-... has been OOMKilled

Details:
  • namespace: prd-mall
  • pod: prd-mall-batch-bbfc6dff9-4q5ds
  • container: prd-mall-batch
  • node: ip-21-101-178-94.ap-northeast-2.compute.internal
  ...
```

### 네이밍 규칙
- `destination_app` 또는 `container` 값: `{env}-{service-name}` 형태 (ex. `prd-mall-backoffice`)
- `namespace`: app 이름의 앞 두 단어 (ex. `prd-mall-backoffice` → namespace: `prd-mall`)

---

## 워크플로우 아키텍처

```
Prometheus AlertManager
        │
        ▼ (Slack 메세지 전송)
  Slack #general
        │
        ▼ (Webhook 수신)
    n8n Trigger
        │
        ▼
  메세지 파싱 노드
  - [FIRING] 여부 확인
  - alertname, service, namespace, severity 추출
  - 알람 타입 분류 (서비스 에러 / 인프라 이벤트)
        │
        ▼
  AI Agent (LLM)
  ┌─────────────────────────────────────┐
  │  사용 가능한 Tool들:                 │
  │  - Loki: 에러 로그 조회              │
  │  - Tempo: TraceID 기반 트레이스 조회 │
  │  - Prometheus: 메트릭 조회           │
  │  - GitHub: 소스코드 조회             │
  │  - GitLab: 인프라 코드 조회          │
  └─────────────────────────────────────┘
        │
        ▼
  분석 결과 요약 (LLM)
        │
        ▼
  Slack 응답 전송
  (원본 메세지 스레드에 댓글 or 채널에 전송)
```

---

## AI Agent 분석 전략

### 알람 타입별 분석 흐름

#### 서비스 에러 (ex. IstioHigh5xxErrorRate)
1. Loki에서 해당 서비스의 에러 로그 조회 (알람 발생 시간 기준 ±10분)
2. 가장 빈번한 에러 패턴 파악
3. 에러 로그에서 TraceID 추출 → Tempo에서 트레이스 조회
4. Prometheus에서 해당 서비스의 5xx rate, latency 메트릭 조회
5. 에러 로그의 스택트레이스/파일 경로 기반으로 GitHub 소스코드 조회
6. GitHub에서 최근 커밋/PR 확인 (배포 연관성 파악)
7. GitLab에서 해당 서비스의 최근 helm values 변경 확인

#### 인프라 이벤트 (ex. OOMKilled, CrashLoopBackOff)
1. Prometheus에서 메모리/CPU 메트릭 조회
2. Loki에서 해당 pod의 로그 조회
3. GitLab에서 해당 서비스의 resource limit 설정 확인 (values.yaml)
4. 필요시 GitHub에서 최근 코드 변경 확인

### namespace 추출 규칙
```
app_name = "prd-mall-backoffice"
namespace = app_name.split("-")[0] + "-" + app_name.split("-")[1]
# → "prd-mall"
```

### Loki 쿼리 패턴
```
{app="prd-mall-backoffice", namespace="prd-mall"} |= "error" | json
```

---

## n8n 노드 구성

### 노드 목록

| 순서 | 노드 타입 | 역할 |
|------|-----------|------|
| 1 | Webhook | Slack Event API 수신 |
| 2 | Code (JS) | 메세지 파싱 및 필드 추출 |
| 3 | IF | [FIRING] 메세지 필터링 |
| 4 | AI Agent | LLM + Tool 기반 분석 |
| 5 | HTTP Request (Tool) | Loki API 호출 |
| 6 | HTTP Request (Tool) | Tempo API 호출 |
| 7 | HTTP Request (Tool) | Prometheus API 호출 |
| 8 | HTTP Request (Tool) | GitHub API 호출 |
| 9 | HTTP Request (Tool) | GitLab API 호출 |
| 10 | Slack | 분석 결과 전송 (스레드 댓글) |

---

## Slack Bot 생성 방법

1. https://api.slack.com/apps 접속
2. "Create New App" → "From scratch"
3. App 이름 입력, 워크스페이스 선택
4. 좌측 메뉴 "OAuth & Permissions" 이동
5. Scopes > Bot Token Scopes 추가:
   - `channels:history` - 채널 메세지 읽기
   - `channels:read` - 채널 정보 읽기
   - `chat:write` - 메세지 전송
   - `chat:write.public` - 공개 채널에 메세지 전송
6. "Install to Workspace" 클릭 → Bot User OAuth Token 복사 (`xoxb-...`)
7. 좌측 메뉴 "Event Subscriptions" 이동
   - Enable Events: ON
   - Request URL: `http://{n8n-host}/webhook/{webhook-id}` 입력
   - Subscribe to bot events: `message.channels` 추가
8. 해당 Bot을 #general 채널에 초대: `/invite @{bot-name}`

---

## 각 서비스 API 연동 스펙

### Loki
- 엔드포인트: EKS 내부 FQDN (ex. `http://loki.monitoring.svc.cluster.local:3100`)
- 쿼리 API: `GET /loki/api/v1/query_range`
- 파라미터: `query`, `start`, `end`, `limit`

### Tempo
- 엔드포인트: EKS 내부 FQDN (ex. `http://tempo.monitoring.svc.cluster.local:3200`)
- TraceID 조회: `GET /api/traces/{traceId}`
- 서비스 검색: `GET /api/search`

### Prometheus
- 엔드포인트: EKS 내부 FQDN (ex. `http://prometheus.monitoring.svc.cluster.local:9090`)
- 쿼리 API: `GET /api/v1/query_range`
- AI가 알람 타입에 따라 적절한 PromQL 생성

### GitHub
- 엔드포인트: `https://api.github.com`
- 인증: PAT (Bearer Token)
- 주요 API:
  - `GET /search/repositories?q={service-name}` - 레포 검색
  - `GET /repos/{owner}/{repo}/commits` - 최근 커밋
  - `GET /repos/{owner}/{repo}/contents/{path}` - 파일 내용

### GitLab
- 엔드포인트: Self-hosted URL
- 인증: Access Token
- 주요 API:
  - `GET /api/v4/projects?search={service-name}` - 프로젝트 검색
  - `GET /api/v4/projects/{id}/repository/commits` - 최근 커밋
  - `GET /api/v4/projects/{id}/repository/files/{file_path}` - 파일 내용

---

## 미확정 항목 (추후 확인 필요)

| 항목 | 내용 |
|------|------|
| LLM 모델 | OpenAI GPT-4o / Claude / 기타 중 선택 필요 |
| Slack 응답 방식 | 스레드 댓글 vs 채널 새 메세지 (현재 스레드 댓글 선호) |
| GitHub org/owner | 레포 검색 시 사용할 organization 이름 |
| GitLab URL | Self-hosted 실제 URL |
| 각 서비스 실제 FQDN | Loki, Tempo, Prometheus 실제 내부 주소 |
| Slack channel ID | #general의 실제 channel ID |

---

## 다음 단계

- [ ] Slack Bot 생성 및 Token 확보
- [ ] n8n 로컬 설치 및 실행
- [ ] n8n workflow JSON 파일 작성
- [ ] 로컬 테스트 (Slack Webhook 수신 확인)
- [ ] 각 Tool 연동 테스트
- [ ] EKS 배포
