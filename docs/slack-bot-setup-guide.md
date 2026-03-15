# Slack Bot 생성 및 n8n 연동 테스트 가이드

## 전체 흐름

```
1. Slack App 생성
      ↓
2. Bot Token 발급
      ↓
3. n8n에 Workflow import
      ↓
4. n8n Webhook URL 확인
      ↓
5. Slack Event Subscriptions 등록
      ↓
6. 테스트 메세지 전송
```

---

## Step 1. Slack App 생성

1. https://api.slack.com/apps 접속
2. **"Create New App"** 클릭
3. **"From scratch"** 선택
4. App Name: `alert-investigator` (원하는 이름)
5. Workspace 선택 → **"Create App"**

---

## Step 2. Bot Token Scopes 설정

1. 좌측 메뉴 **"OAuth & Permissions"** 클릭
2. **"Bot Token Scopes"** 섹션에서 **"Add an OAuth Scope"** 클릭
3. 아래 4개 추가:
   - `channels:history`
   - `channels:read`
   - `chat:write`
   - `chat:write.public`

---

## Step 3. Workspace에 설치 및 Token 복사

1. 같은 페이지 상단 **"Install to Workspace"** 클릭
2. 권한 허용
3. **"Bot User OAuth Token"** 복사 (`xoxb-...` 형태)
   - 이 값이 n8n Slack credential에 들어갈 값

---

## Step 4. n8n에 Workflow Import

1. `http://localhost:5678` 접속
2. 좌측 **"Workflows"** → 우측 상단 **"..."** 또는 **"Add workflow"** 옆 메뉴
3. **"Import from file"** → `n8n-alert-workflow.json` 선택

---

## Step 5. n8n Slack Credential 등록

1. n8n 좌측 메뉴 **"Credentials"** → **"Add credential"**
2. **"Slack"** 검색 → **"Slack OAuth2 API"** 선택
3. Access Token에 Step 3에서 복사한 `xoxb-...` 토큰 입력
4. **"Save"**
5. Workflow로 돌아가서 **"Slack Reply"** 노드 클릭 → Credential 선택

---

## Step 6. n8n Gemini Credential 등록

1. n8n **"Credentials"** → **"Add credential"**
2. **"Google Gemini(PaLm) Api"** 검색 → 선택
3. API Key 입력 (Google AI Studio에서 발급한 키)
4. **"Save"**
5. Workflow에서 **"Gemini Model"** 노드 클릭 → Credential 선택

---

## Step 7. n8n Webhook URL 확인

1. Workflow에서 **"Slack Webhook"** 노드 클릭
2. **"Test URL"** 또는 **"Production URL"** 확인
   - 테스트: `http://localhost:5678/webhook-test/slack-alert`
   - 운영: `http://localhost:5678/webhook/slack-alert`

> ⚠️ 로컬 localhost는 Slack에서 직접 접근 불가. 아래 ngrok 설정 필요.

---

## Step 8. ngrok으로 로컬 터널링 (로컬 테스트용)

Slack Event API는 외부에서 접근 가능한 HTTPS URL이 필요해요.

```bash
# ngrok 설치 (없다면)
# https://ngrok.com/download 에서 다운로드

# 터널 실행
ngrok http 5678
```

실행 후 출력되는 URL 복사:
```
Forwarding  https://xxxx-xxxx.ngrok-free.app -> http://localhost:5678
```

Webhook URL 완성:
```
https://xxxx-xxxx.ngrok-free.app/webhook/slack-alert
```

---

## Step 9. Slack Event Subscriptions 등록

1. https://api.slack.com/apps → 생성한 앱 선택
2. 좌측 **"Event Subscriptions"** 클릭
3. **"Enable Events"** → ON
4. **"Request URL"** 에 ngrok URL 입력:
   ```
   https://xxxx-xxxx.ngrok-free.app/webhook/slack-alert
   ```
   - n8n workflow가 **활성화(Active)** 상태여야 Slack이 URL 검증 가능
   - `Verified ✓` 표시 확인
5. **"Subscribe to bot events"** → **"Add Bot User Event"**
   - `message.channels` 추가
6. **"Save Changes"**

> ⚠️ URL 검증 전에 n8n workflow를 먼저 Active로 켜두어야 해요.

---

## Step 10. Bot을 채널에 초대

Slack 앱에서 #general 채널로 이동 후:

```
/invite @alert-investigator
```

---

## Step 11. 테스트

### n8n Workflow 활성화
1. n8n에서 workflow 열기
2. 우측 상단 토글 → **Active** 로 변경

### 테스트 메세지 전송
#general 채널에 아래 메세지 직접 입력:

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

### 확인 포인트
- n8n **"Executions"** 탭에서 실행 로그 확인
- AI Agent가 각 Tool을 호출하는 과정 확인
- Slack #general 채널에 해당 메세지 스레드로 분석 결과 댓글 확인

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| Slack URL 검증 실패 | n8n workflow가 inactive | workflow Active로 변경 후 재시도 |
| Slack URL 검증 실패 | ngrok URL 만료 | ngrok 재실행 후 URL 재등록 |
| AI Agent 응답 없음 | Gemini credential 미설정 | Gemini Model 노드 credential 확인 |
| Loki/Prometheus 에러 | placeholder URL 그대로 | 로컬 테스트 시 해당 Tool 노드 비활성화 가능 |
| Bot이 메세지 못 받음 | 채널에 Bot 미초대 | `/invite @bot-name` 실행 |
| 무한 루프 | Bot 자신의 메세지에 반응 | 파싱 노드에서 `event.bot_id` 필터 자동 처리됨 |

---

## 로컬 테스트 시 Loki/Prometheus 없이 테스트하는 방법

실제 인프라 없이 AI Agent 동작만 확인하고 싶다면:

1. n8n workflow에서 **Tool: Loki**, **Tool: Prometheus**, **Tool: Tempo**, **Tool: GitLab** 노드를 AI Agent에서 연결 해제
2. **Tool: GitHub** 만 연결한 상태로 테스트
3. GitHub PAT만 있으면 실제 GitHub API 호출 테스트 가능

또는 각 Tool 노드의 URL을 mock 서버로 교체해서 테스트도 가능.
