#!/bin/bash
# 에러 스파이크: 각 서비스 /simulate/error 반복 호출
# 예상 알람: IstioHigh5xxErrorRate (5xx 에러율 > 10%)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

k6 run \
  -e USER_SERVICE="${USER_SERVICE:-http://localhost:18083}" \
  -e PRODUCT_SERVICE="${PRODUCT_SERVICE:-http://localhost:18081}" \
  -e ORDER_SERVICE="${ORDER_SERVICE:-http://localhost:18082}" \
  -e REVIEW_SERVICE="${REVIEW_SERVICE:-http://localhost:18084}" \
  "$SCRIPT_DIR/scenarios/02-error-spike.js"
