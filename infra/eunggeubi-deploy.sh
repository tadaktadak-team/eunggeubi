#!/bin/bash
# S3 의 JAR 을 받아 새 릴리스로 배포한다. 헬스체크 실패 시 이전 릴리스로 자동 롤백.
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "sudo 로 실행하세요"; exit 1; }

BUCKET=eunggeubi-artifacts-539839664793
REGION=ap-northeast-2
BASE=/opt/eunggeubi
KEY=${1:-app.jar}
KEEP=3              # current 를 포함해 남겨 둘 릴리스 수
MIN_FREE_MB=400     # JAR(약 110MB) + 앱이 돌면서 쓰는 여유
TS=$(date +%Y%m%d-%H%M%S)
REL="$BASE/releases/$TS"
PREV=$(readlink -e "$BASE/current" 2>/dev/null || true)

# 최신 순으로 훑으면서 current 는 건너뛰고, 그 밖의 릴리스는 ($1 - 1)개만 남긴다
prune_releases() {
  local keep=$1 cur n=0 d
  cur=$(readlink -e "$BASE/current" 2>/dev/null || true)
  for d in $(ls -1dt "$BASE"/releases/*/ 2>/dev/null || true); do
    d=${d%/}
    [ "$d" = "$cur" ] && continue
    n=$((n + 1))
    [ "$n" -lt "$keep" ] || rm -rf "$d"
  done
}

# 실패로 끝나면 새 릴리스 폴더를 지운다. 예전에는 실패할 때마다 JAR 이 하나씩 남아 디스크가 찼다.
# current 가 새 릴리스를 가리키면(롤백할 이전 릴리스가 없던 경우) 지금 돌고 있으므로 남긴다.
cleanup_on_exit() {
  if [ "$(readlink -e "$BASE/current" 2>/dev/null || true)" != "$REL" ]; then
    rm -rf "$REL"
  fi
}
trap cleanup_on_exit EXIT

echo "[0/5] 디스크 확인"
prune_releases $((KEEP - 1))   # 새 릴리스가 들어올 자리를 먼저 만든다
FREE_MB=$(df -Pm "$BASE" | awk 'NR==2 {print $4}')
if [ "$FREE_MB" -lt "$MIN_FREE_MB" ]; then
  echo "[중단] 디스크 여유 ${FREE_MB}MB (최소 ${MIN_FREE_MB}MB 필요). 서버는 지금 버전 그대로입니다."
  echo "  확인: df -h /   sudo du -xh --max-depth=2 / | sort -rh | head -25"
  exit 1
fi
echo "  여유 ${FREE_MB}MB"

echo "[1/5] s3://$BUCKET/$KEY 내려받는 중"
install -d -o eunggeubi -g eunggeubi -m 0750 "$REL"
aws s3 cp "s3://$BUCKET/$KEY" "$REL/app.jar" --region "$REGION" --only-show-errors
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

echo "[5/5] 오래된 릴리스 정리 (최근 ${KEEP}개만 유지)"
prune_releases "$KEEP"
echo "배포 완료: $TS"
