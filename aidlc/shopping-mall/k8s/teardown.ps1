# Shopping Mall K8s 전체 삭제 스크립트 (PowerShell)
Write-Host "=== Shopping Mall K8s 전체 삭제 ===" -ForegroundColor Cyan
kubectl delete namespace shopping-mall --ignore-not-found
Write-Host "삭제 완료" -ForegroundColor Green
