# n8n Alert Investigation Workflow

## 개요

Prometheus AlertManager가 Slack에 알람을 보내면, n8n이 이를 감지하고 AI Agent가 자동으로 원인을 분석하여 Slack 스레드에 결과를 회신하는 워크플로우입니다.

---

## 전체 흐름

```
AlertManager → Slack #alerts 채널
                  │
                  ▼
            n8n Webhook Trigger (Slack Event API)
                  │
                  ▼
            ① Webhook Response (Slack challenge 응답)
                  │
                  ▼
            ② Parse Alert Message (Code 노드)
               - [FIRING] 여부 확인
               - alertName, service, namespace, severity 추출
                  │
                  ▼
            ③ Is FIRING? (IF 노드)
               - skip=true → 종료
               - skip=false → 다음 단계
                  │
                  ▼
            ④ Enrich Context (Code 노드) ← ★ 핵심
               - service 이름으로 메타데이터 매핑
               - GitHub 레포, namespace, Loki 라벨 등
               - AI에게 전달할 컨텍스트 조립
                  │
                  ▼
            ⑤ AI Investigation Agent
               - LLM + Tool 기반 분석
               - Loki, Prometheus, Tempo, GitHub 조회
                  │
                  ▼
            ⑥ Slack Reply (스레드 댓글)
```

---

## 노드별 상세 설명

### ① Slack Webhook (Trigger)

n8n의 Webhook 노드로 Slack Event API를 수신합니다.

- 타입: `Webhook`
- Method: `POST`
- Path: `slack-alert`
- Response Mode: `Respond to Webhook` (별도 Response 노드 사용)

Slack Event API는 처음 등록할 때 `url_verification` challenge를 보내요. 이걸 응답해줘야 등록이 완료됩니다.

### ② Webhook Response

Slack challenge에 즉시 응답하는 노드입니다. Slack은 3초 안에 응답을 받아야 해요.

- `body.challenge` 값이 있으면 그대로 반환
- 없으면 `ok` 반환

### ③ Parse Alert Message (Code 노드)

Slack 메시지에서 알람 정보를 파싱합니다.

추출하는 필드:
- `alertName`: 알람 이름 (IstioHigh5xxErrorRate, HighMemoryUsage 등)
- `appName`: 서비스 이름 (destination_app 또는 service 라벨)
- `namespace`: 네임스페이스
- `severity`: 심각도
- `alertType`: service / infra 분류
- `startTime`, `endTime`: 알람 시간 ±10분 (Loki/Prometheus 조회용)
- `channel`, `ts`: Slack 스레드 응답용

### ④ Enrich Context (Code 노드) — ★ 핵심

서비스 이름을 기반으로 AI에게 전달할 메타데이터를 매핑하는 노드입니다.
AI가 "이 서비스는 어떤 레포를 봐야 하는지" 같은 걸 모르니까, 여기서 미리 알려주는 거예요.

```javascript
// Enrich Context — 서비스별 메타데이터 매핑
const alert = $input.first().json;

// ===== 서비스 메타데이터 맵 =====
// 새 서비스 추가 시 여기만 수정하면 됩니다
const SERVICE_MAP = {
  'shop-user': {
    github: { owner: 'sdjang6253', repo: 'n8n-with-ai-dlc', path: 'aidlc/shopping-mall/shop-user' },
    namespace: 'shopping-mall',
    lokiLabels: '{app="shop-user", namespace="shopping-mall"}',
    prometheusJob: 'shop-user',
    description: '회원가입/로그인/JWT 발급 서비스',
  },
  'shop-product': {
    github: { owner: 'sdjang6253', repo: 'n8n-with-ai-dlc', path: 'aidlc/shopping-mall/shop-product' },
    namespace: 'shopping-mall',
    lokiLabels: '{app="shop-product", namespace="shopping-mall"}',
    prometheusJob: 'shop-product',
    description: '상품 목록/상세/재고 관리 서비스',
  },
  'shop-order': {
    github: { owner: 'sdjang6253', repo: 'n8n-with-ai-dlc', path: 'aidlc/shopping-mall/shop-order' },
    namespace: 'shopping-mall',
    lokiLabels: '{app="shop-order", namespace="shopping-mall"}',
    prometheusJob: 'shop-order',
    description: '장바구니/주문/리뷰 Kafka Producer 서비스',
  },
  'shop-review': {
    github: { owner: 'sdjang6253', repo: 'n8n-with-ai-dlc', path: 'aidlc/shopping-mall/shop-review' },
    namespace: 'shopping-mall',
    lokiLabels: '{app="shop-review", namespace="shopping-mall"}',
    prometheusJob: 'shop-review',
    description: '리뷰 Kafka Consumer/조회 서비스',
  },
  'shop-frontend': {
    github: { owner: 'sdjang6253', repo: 'n8n-with-ai-dlc', path: 'aidlc/shopping-mall/shop-frontend' },
    namespace: 'shopping-mall',
    lokiLabels: '{app="shop-frontend", namespace="shopping-mall"}',
    prometheusJob: 'shop-frontend',
    description: 'React + Vite + nginx 프론트엔드',
  },
  // ===== 회사 서비스 예시 (추후 추가) =====
  // 'prd-mall-backoffice': {
  //   github: { owner: 'your-org', repo: 'mall-backoffice', path: 'src' },
  //   namespace: 'prd-mall',
  //   lokiLabels: '{app="prd-mall-backoffice", namespace="prd-mall"}',
  //   prometheusJob: 'prd-mall-backoffice',
  //   description: '백오피스 서비스',
  // },
};

// 서비스 매칭 (정확히 일치 → 부분 일치 순)
const appName = alert.appName || '';
let meta = SERVICE_MAP[appName];
if (!meta) {
  // 부분 매칭 시도 (prd-mall-backoffice → shop-order 같은 경우)
  const key = Object.keys(SERVICE_MAP).find(k => appName.includes(k));
  meta = key ? SERVICE_MAP[key] : null;
}

// 매칭 실패 시 기본값
if (!meta) {
  meta = {
    github: { owner: 'sdjang6253', repo: 'n8n-with-ai-dlc', path: '' },
    namespace: alert.namespace || 'unknown',
    lokiLabels: `{namespace="${alert.namespace || 'unknown'}"}`,
    prometheusJob: appName,
    description: '매핑되지 않은 서비스',
  };
}

return [{
  json: {
    ...alert,
    // 원본 알람 정보 유지 + 메타데이터 추가
    github: meta.github,
    lokiLabels: meta.lokiLabels,
    prometheusJob: meta.prometheusJob,
    serviceDescription: meta.description,
    resolvedNamespace: meta.namespace,
  }
}];
```

이 노드의 핵심 가치:
- AI가 "shop-user 서비스의 GitHub 레포가 뭐야?" 같은 삽질을 안 해도 됨
- 서비스가 추가되면 `SERVICE_MAP`에 한 줄만 추가하면 됨
- 회사 서비스와 실습 서비스를 같은 워크플로우에서 처리 가능
- Loki 라벨, Prometheus job 이름 등 각 서비스마다 다를 수 있는 값을 미리 정리

### ⑤ AI Investigation Agent

n8n의 AI Agent 노드입니다. LLM에게 알람 정보 + 메타데이터를 전달하고, Tool을 사용해서 직접 조사하게 합니다.

#### System Prompt

```
You are an expert SRE AI assistant. You investigate Prometheus alerts by querying
observability tools and source code. Be concise, factual, and actionable.

Rules:
- Always start with Loki logs to understand what happened
- Use the provided lokiLabels and prometheusJob — don't guess label names
- If you find a traceId in logs, query Tempo for the full trace
- Check GitHub for recent commits only if logs suggest a code-level issue
- Respond in Korean (한국어)
- Keep the analysis under 2000 characters for Slack readability
```

#### User Prompt (④에서 전달받은 데이터 사용)

```
다음 Prometheus 알람을 분석해주세요.

## 알람 정보
- 알람: {{ $json.alertName }}
- 타입: {{ $json.alertType }}
- 서비스: {{ $json.appName }} ({{ $json.serviceDescription }})
- 네임스페이스: {{ $json.resolvedNamespace }}
- 심각도: {{ $json.severity }}
- 시간 범위: {{ $json.startTimeMs }} ~ {{ $json.endTimeMs }} (unix seconds)

## 조회 힌트
- Loki 라벨: {{ $json.lokiLabels }}
- Prometheus job: {{ $json.prometheusJob }}
- GitHub: {{ $json.github.owner }}/{{ $json.github.repo }} (경로: {{ $json.github.path }})

## 원본 알람
{{ $json.raw }}

## 분석 포맷
🚨 알람 요약
🔍 원인 분석 (Loki 로그 기반)
📊 메트릭 현황 (Prometheus 기반)
🔗 관련 트레이스 (있는 경우)
💡 조치 권고
```

#### AI Agent에 연결되는 Tool들

| Tool | 타입 | 용도 |
|------|------|------|
| Loki | HTTP Request Tool | 에러 로그 조회 |
| Prometheus | HTTP Request Tool | 메트릭 조회 (에러율, 레이턴시, 메모리 등) |
| Tempo | HTTP Request Tool | TraceID 기반 분산 추적 |
| GitHub | HTTP Request Tool | 소스코드, 최근 커밋 조회 |

각 Tool은 AI가 파라미터를 동적으로 생성합니다 (`$fromAI()` 사용).

### ⑥ Slack Reply

AI 분석 결과를 원본 알람 메시지의 스레드에 댓글로 전송합니다.

- `channel`: ③에서 추출한 Slack channel ID
- `thread_ts`: ③에서 추출한 원본 메시지 timestamp
- `text`: AI Agent의 output

스레드 댓글로 가기 때문에 채널이 지저분해지지 않아요.

---

## Tool 상세 설정

### Loki Tool

```
Name: query_loki_logs
Description: Query Loki for application logs. Use the provided lokiLabels as base
             and append filters like |= "error" or | json. Always use the provided
             start/end time range.
Method: GET
URL: http://loki.monitoring.svc.cluster.local:3100/loki/api/v1/query_range
Query Parameters:
  - query: $fromAI('logql_query')    // AI가 LogQL 생성
  - start: $fromAI('start_time_ns')  // 나노초
  - end: $fromAI('end_time_ns')      // 나노초
  - limit: 100
```

로컬 테스트 시 URL: `http://localhost:13100/loki/api/v1/query_range`

### Prometheus Tool

```
Name: query_prometheus_metrics
Description: Query Prometheus for metrics. Use the provided prometheusJob label
             in your PromQL. Common queries:
             - Error rate: rate(http_server_requests_seconds_count{job="X",status=~"5.."}[5m])
             - Latency p99: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="X"}[5m]))
             - Memory: jvm_memory_used_bytes{job="X",area="heap"}
Method: GET
URL: http://prometheus.monitoring.svc.cluster.local:9090/api/v1/query_range
Query Parameters:
  - query: $fromAI('promql')
  - start: $fromAI('start_unix')  // unix seconds
  - end: $fromAI('end_unix')      // unix seconds
  - step: 60
```

로컬 테스트 시 URL: `http://localhost:19090/api/v1/query_range`

### Tempo Tool

```
Name: query_tempo_trace
Description: Query Tempo for a distributed trace by traceId.
             Only use this when you find a traceId in Loki logs.
             Tempo responses can be large — focus on error spans.
Method: GET
URL: http://tempo.monitoring.svc.cluster.local:3200/api/traces/{traceId}
```

> Tempo는 응답이 클 수 있어서, AI에게 "에러 span 위주로 봐라"고 system prompt에서 안내합니다.

### GitHub Tool

```
Name: search_github_code
Description: Search GitHub for source code, recent commits, or file contents.
             Use the provided github owner/repo/path information.
             Common API paths:
             - Recent commits: repos/{owner}/{repo}/commits?path={path}&per_page=5
             - File content: repos/{owner}/{repo}/contents/{path}/{filename}
             - Search code: search/code?q={error_class}+repo:{owner}/{repo}
Method: GET
URL: https://api.github.com/{api_path}
Headers:
  - Authorization: Bearer {GITHUB_PAT}
  - Accept: application/vnd.github+json
```

---

## n8n에서 만드는 순서 (처음 하는 사람용)

### 1단계: n8n 설치 및 실행

```bash
# Docker로 실행 (가장 간단)
docker run -it --rm \
  --name n8n \
  -p 5678:5678 \
  -v n8n_data:/home/node/.n8n \
  n8nio/n8n
```

브라우저에서 `http://localhost:5678` 접속 → 계정 생성

### 2단계: Credential 등록

n8n 좌측 메뉴 → Settings → Credentials에서 등록:

| Credential | 타입 | 값 |
|---|---|---|
| Slack OAuth2 | Slack OAuth2 API | Bot User OAuth Token (`xoxb-...`) |
| Google Gemini | Google Gemini API | API Key |
| GitHub PAT | Header Auth | `Authorization: Bearer ghp_...` |

> LLM은 Gemini 외에 OpenAI, Claude 등 원하는 걸로 교체 가능합니다.

### 3단계: 워크플로우 생성

n8n 캔버스에서 노드를 하나씩 추가합니다.

#### 3-1. Webhook 노드 추가
- 캔버스 빈 곳 클릭 → `Webhook` 검색 → 추가
- Method: POST
- Path: `slack-alert`
- Response Mode: `Respond to Webhook`

#### 3-2. Respond to Webhook 노드
- Webhook에서 선 연결
- Response Body: `{{ $json.challenge ?? 'ok' }}`

#### 3-3. Code 노드 (Parse Alert Message)
- Webhook에서 선 연결 (Response와 병렬)
- Language: JavaScript
- 위의 Parse Alert Message 코드 붙여넣기

#### 3-4. IF 노드 (Is FIRING?)
- Parse에서 선 연결
- Condition: `{{ $json.skip }}` equals `true`
- True → 아무것도 연결 안 함 (종료)
- False → 다음 노드로

#### 3-5. Code 노드 (Enrich Context)
- IF의 False 출력에서 선 연결
- 위의 Enrich Context 코드 붙여넣기

#### 3-6. AI Agent 노드
- Enrich Context에서 선 연결
- Agent Type: Tools Agent
- System Message, User Prompt 위의 내용 붙여넣기
- 하단에 LLM 모델 연결 (Gemini / OpenAI / Claude)
- 하단에 Tool 노드들 연결

#### 3-7. Tool 노드들 (Loki, Prometheus, Tempo, GitHub)
- 각각 `HTTP Request Tool` 노드로 추가
- AI Agent 하단의 Tool 슬롯에 연결
- 위의 Tool 상세 설정 참고하여 URL, 파라미터 설정

#### 3-8. Slack 노드 (Reply)
- AI Agent에서 선 연결
- Resource: Message → Send
- Channel: `{{ $('Parse Alert Message').item.json.channel }}`
- Text: `{{ $json.output }}`
- Thread TS: `{{ $('Parse Alert Message').item.json.ts }}`

### 4단계: Slack Bot 설정

1. https://api.slack.com/apps → Create New App → From scratch
2. OAuth & Permissions → Bot Token Scopes 추가:
   - `channels:history` — 채널 메시지 읽기
   - `channels:read` — 채널 정보 읽기
   - `chat:write` — 메시지 전송
3. Install to Workspace → Bot User OAuth Token 복사 (`xoxb-...`)
4. Event Subscriptions → Enable Events: ON
   - Request URL: `http://{n8n-host}/webhook/slack-alert`
   - Subscribe to bot events: `message.channels`
5. 알람 채널에 Bot 초대: `/invite @{bot-name}`

### 5단계: 테스트

```bash
# AlertManager 시뮬레이션 (Docker Compose 환경)
curl http://localhost:18081/simulate/error
# → Prometheus 알람 발생 → AlertManager → Slack → n8n → AI 분석 → Slack 스레드 응답
```

---

## 확장 시나리오

### 스케줄 기반 점검 (추후)

```
Schedule Trigger (매시간)
    │
    ▼
Code 노드: PromQL 생성
  - CPU 사용률 > 80%
  - 메모리 사용률 > 85%
  - 에러율 > 3%
    │
    ▼
HTTP Request: Prometheus 조회
    │
    ▼
IF: 임계치 초과?
    │
    ▼ (Yes)
AI Agent: 분석
    │
    ▼
Slack: 리포트 전송
```

이건 별도 워크플로우로 만들면 돼요. AlertManager를 거치지 않고 n8n이 직접 Prometheus를 폴링하는 방식입니다.

### 에러율 임계치 낮추기 (3%)

Prometheus alert rule을 수정하면 됩니다:

```yaml
# 현재: > 10%
- alert: IstioHigh5xxErrorRate
  expr: ... > 0.1

# 변경: > 3%
- alert: IstioHigh5xxErrorRate
  expr: ... > 0.03
```

워크플로우 자체는 변경 불필요 — 알람이 더 자주 올 뿐이에요.

---

## 워크플로우 JSON

이 문서의 워크플로우를 n8n에 import할 수 있는 JSON은 `docs/n8n-alert-workflow.json`에 있습니다.
n8n 캔버스 → 우측 상단 `...` → Import from File로 가져올 수 있어요.

import 후 반드시 수정해야 하는 값:
- `LOKI_FQDN` → 실제 Loki 주소
- `PROMETHEUS_FQDN` → 실제 Prometheus 주소
- `TEMPO_FQDN` → 실제 Tempo 주소
- `GITHUB_PAT_TOKEN` → 실제 GitHub PAT
- Credential ID들 → n8n에서 생성한 Credential로 교체

---

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                    Slack #alerts                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │ [FIRING:1] IstioHigh5xxErrorRate                 │   │
│  │ Service: shop-product                            │   │
│  │ ...                                              │   │
│  └──────────────────────────────────────────────────┘   │
│         │                              ▲                 │
│         │ Event API                    │ 스레드 댓글     │
│         ▼                              │                 │
│  ┌─────────────────────────────────────┴──────────────┐ │
│  │                    n8n Workflow                     │ │
│  │                                                    │ │
│  │  Webhook → Parse → IF → Enrich → AI Agent → Reply │ │
│  │                                      │             │ │
│  │                              ┌───────┼───────┐     │ │
│  │                              ▼       ▼       ▼     │ │
│  │                           Loki  Prometheus GitHub   │ │
│  │                              ▼                     │ │
│  │                           Tempo                    │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```
