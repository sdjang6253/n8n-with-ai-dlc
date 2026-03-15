# 에러 스파이크: 각 서비스 /simulate/error 반복 호출
# 예상 알람: IstioHigh5xxErrorRate (5xx 에러율 > 10%)

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
  "$ScriptDir/scenarios/02-error-spike.js"
