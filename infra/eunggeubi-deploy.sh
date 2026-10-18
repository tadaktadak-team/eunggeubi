#!/bin/bash
# S3 의 JAR 을 받아 새 릴리스로 배포한다. 헬스체크 실패 시 이전 릴리스로 자동 롤백.
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "sudo 로 실행하세요"; exit 1; }

BUCKET=eunggeubi-artifacts-539839664793
REGION=ap-northeast-2
BASE=/opt/eunggeubi
KEY=${1:-app.jar}
TS=$(date +%Y%m%d-%H%M%S)
REL="$BASE/releases/$TS"
PREV=$(readlink -f "$BASE/current" 2>/dev/null || true)

echo "[1/5] s3://$BUCKET/$KEY 내려받는 중"
install -d -o eunggeubi -g eunggeubi -m 0750 "$REL"
aws s3 cp "s3://$BUCKET/$KEY" "$REL/app.jar" --region "$REGION"
chown eunggeubi:eunggeubi "$REL/app.jar"

echo "[2/5] current -> $REL"
ln -sfn "$REL" "$BASE/current.new"
mv -Tf "$BASE/current.new" "$BASE/current"
chown -h eunggeubi:eunggeubi "$BASE/current"

echo "[3/5] 서비스 재시작"
systemctl restart eunggeubi

echo "[4/5] 헬스체크 (최대 120초)"
OK=0
for i in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8080/actuator/health/readiness >/dev/null 2>&1; then OK=1; echo "  준비됨 (${i}회차)"; break; fi
  sleep 2
done

if [ "$OK" -ne 1 ]; then
  echo "[실패] 헬스체크 실패"
  if [ -n "$PREV" ] && [ -d "$PREV" ] && [ "$PREV" != "$REL" ]; then
    echo "  이전 릴리스로 롤백: $PREV"
    ln -sfn "$PREV" "$BASE/current.new"; mv -Tf "$BASE/current.new" "$BASE/current"
    systemctl restart eunggeubi
  fi
  echo "  로그: sudo journalctl -u eunggeubi -n 120 --no-pager"
  exit 1
fi

echo "[5/5] 오래된 릴리스 정리 (최근 3개만 유지)"
ls -1dt "$BASE"/releases/*/ 2>/dev/null | tail -n +4 | xargs -r rm -rf
echo "배포 완료: $TS"
