# 메모리 부하: /simulate/memory 반복 호출
# 예상 알람: HighMemoryUsage (JVM 힙 사용률 > 85%)
# 사용법: .\run-03-memory-load.ps1 [서비스URL]
#   예시: .\run-03-memory-load.ps1 http://localhost:18082

param(
  [string]$TargetUrl = ""
)

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

# 인자 > SERVICE 환경변수 > PRODUCT_SERVICE > 기본값 순으로 우선순위
if ($TargetUrl) {
  $Target = $TargetUrl
} elseif ($env:SERVICE) {
  $Target = $env:SERVICE
} elseif ($env:PRODUCT_SERVICE) {
  $Target = $env:PRODUCT_SERVICE
} else {
  $Target = "http://localhost:18081"
}

Write-Host "대상 서비스: $Target"

k6 run `
  -e "SERVICE=$Target" `
  "$ScriptDir/scenarios/03-memory-load.js"
