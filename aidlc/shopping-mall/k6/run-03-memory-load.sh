#!/bin/bash
# 메모리 부하: /simulate/memory 반복 호출
# 예상 알람: HighMemoryUsage (JVM 힙 사용률 > 85%)
# 사용법: bash run-03-memory-load.sh [서비스URL]
#   예시: bash run-03-memory-load.sh http://localhost:18082

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET="${1:-${SERVICE:-http://localhost:18081}}"

echo "대상 서비스: $TARGET"

k6 run \
  -e SERVICE="$TARGET" \
  "$SCRIPT_DIR/scenarios/03-memory-load.js"
