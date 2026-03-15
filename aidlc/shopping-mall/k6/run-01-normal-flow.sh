#!/bin/bash
# 정상 플로우: 로그인 → 상품 조회 → 장바구니 → 주문 → 리뷰
# 예상 결과: 알람 없음, p95 < 1000ms

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# .env 로드 (존재하는 경우)
if [ -f "$SCRIPT_DIR/.env" ]; then
  set -a
  source "$SCRIPT_DIR/.env"
  set +a
fi

k6 run \
  -e USER_SERVICE="${USER_SERVICE:-http://localhost:18083}" \
  -e PRODUCT_SERVICE="${PRODUCT_SERVICE:-http://localhost:18081}" \
  -e ORDER_SERVICE="${ORDER_SERVICE:-http://localhost:18082}" \
  -e REVIEW_SERVICE="${REVIEW_SERVICE:-http://localhost:18084}" \
  "$SCRIPT_DIR/scenarios/01-normal-flow.js"
