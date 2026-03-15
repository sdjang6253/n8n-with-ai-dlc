#!/bin/bash
echo "=== Shopping Mall K8s 전체 삭제 ==="
kubectl delete namespace shopping-mall --ignore-not-found
echo "삭제 완료"
