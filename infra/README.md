# infra

서버에서 실제로 동작 중인 설정을 저장소로 옮겨 둔 것입니다.
README가 설명하는 보안·배포 동작의 근거가 되는 파일들입니다.

| 파일 | 서버 경로 | 역할 |
|---|---|---|
| `Caddyfile` | `/etc/caddy/Caddyfile` | 자동 HTTPS(Let's Encrypt), `127.0.0.1:8080` 리버스 프록시, `/actuator/*` 404 차단, 보안 헤더(HSTS·nosniff·Referrer-Policy·`-Server`) |
| `eunggeubi.service` | `/etc/systemd/system/eunggeubi.service` | 전용 계정 실행, 자동 재시작, systemd 샌드박싱(`NoNewPrivileges`·`ProtectSystem=full` 등) |
| `eunggeubi-run.sh` | `/usr/local/bin/eunggeubi-run.sh` | SSM Parameter Store(`/eunggeubi/prod/`)에서 시크릿을 환경변수로만 주입하고 JVM 실행. 평문 `.env`를 디스크에 남기지 않음 |
| `eunggeubi-deploy.sh` | `/usr/local/bin/eunggeubi-deploy.sh` | 디스크 여유 확인(400MB 미만이면 중단) → S3 JAR 내려받기 → 릴리스 생성 → 심링크 원자적 교체 → 헬스체크(최대 120초) → 실패 시 이전 릴리스 롤백 + 새 릴리스 폴더 삭제 → 최근 3개만 보관 |

GitHub Actions(`.github/workflows/deploy.yml`)가 SSM `send-command`로 `eunggeubi-deploy.sh`를 호출합니다.

## 수정할 때

**이 저장소가 진본입니다.** 서버에서 직접 고치면 저장소와 어긋나므로,
변경은 여기서 하고 서버에 반영한 뒤 같은 내용으로 커밋해 주세요.

```bash
# 예: Caddy 설정 반영
sudo cp Caddyfile /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

비밀값은 이 디렉터리에 두지 않습니다. 애플리케이션 시크릿은 SSM Parameter Store(SecureString)에서 기동 시 주입됩니다.
