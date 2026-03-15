---
inclusion: always
---

# README 동기화 규칙

`aidlc/` 하위 프로젝트에서 아래 작업이 발생하면 `aidlc/README.md`를 반드시 함께 업데이트한다.

## 업데이트가 필요한 경우

- 새 프로젝트 또는 서비스 추가
- 포트 번호 변경
- 기동 방법 / 명령어 변경
- 테스트 실행 방법 변경
- 새로운 엔드포인트 또는 시뮬레이션 기능 추가
- 기술 스택 변경 (의존성, 프레임워크 버전 등)
- docker-compose.yml 구조 변경

## 규칙

- README는 `aidlc/README.md` 하나로 유지한다 (각 프로젝트 하위에 별도 README 불필요)
- 명령어는 `aidlc/shopping-mall` 기준 상대 경로로 작성한다
- 접속 URL 표는 항상 최신 포트를 반영한다
