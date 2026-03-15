# 정상 플로우: 로그인 → 상품 조회 → 장바구니 → 주문 → 리뷰
# 예상 결과: 알람 없음, p95 < 1000ms

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# .env 로드 (존재하는 경우)
$EnvFile = Join-Path $ScriptDir ".env"
if (Test-Path $EnvFile) {
  Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
      [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
    }
  }
}

$UserService    = if ($env:USER_SERVICE)    { $env:USER_SERVICE }    else { "http://localhost:18083" }
$ProductService = if ($env:PRODUCT_SERVICE) { $env:PRODUCT_SERVICE } else { "http://localhost:18081" }
$OrderService   = if ($env:ORDER_SERVICE)   { $env:ORDER_SERVICE }   else { "http://localhost:18082" }
$ReviewService  = if ($env:REVIEW_SERVICE)  { $env:REVIEW_SERVICE }  else { "http://localhost:18084" }

k6 run `
  -e "USER_SERVICE=$UserService" `
  -e "PRODUCT_SERVICE=$ProductService" `
  -e "ORDER_SERVICE=$OrderService" `
  -e "REVIEW_SERVICE=$ReviewService" `
  "$ScriptDir/scenarios/01-normal-flow.js"
