#!/bin/bash
# 레이턴시 스파이크: /simulate/slow 반복 호출
# 예상 알람: HighLatency (HTTP p99 > 2000ms)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

k6 run \
  -e USER_SERVICE="${USER_SERVICE:-http://localhost:18083}" \
  -e PRODUCT_SERVICE="${PRODUCT_SERVICE:-http://localhost:18081}" \
  -e ORDER_SERVICE="${ORDER_SERVICE:-http://localhost:18082}" \
  -e REVIEW_SERVICE="${REVIEW_SERVICE:-http://localhost:18084}" \
  "$SCRIPT_DIR/scenarios/04-latency-spike.js"
