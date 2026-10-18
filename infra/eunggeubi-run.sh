#!/bin/bash
# Parameter Store 에서 시크릿을 받아 "환경변수로만" 올리고 앱을 실행한다.
# 평문 .env 를 디스크에 남기지 않는 것이 이 방식의 핵심이다.
set -euo pipefail

REGION=ap-northeast-2
PREFIX=/eunggeubi/prod/
JAR=/opt/eunggeubi/current/app.jar

eval "$(aws ssm get-parameters-by-path \
    --region "$REGION" --path "$PREFIX" --recursive --with-decryption \
    --output json \
  | jq -r '.Parameters[] | "export " + (.Name | split("/") | last) + "=" + (.Value | @sh)')"

# 핵심 값이 비면 조용히 뜨지 말고 즉시 죽는다 (기본값으로 떠버리는 사고 방지)
: "${JWT_SECRET:?Parameter Store 에서 JWT_SECRET 을 못 받았습니다}"
: "${DB_HOST:?Parameter Store 에서 DB_HOST 를 못 받았습니다}"
: "${DB_PASSWORD:?Parameter Store 에서 DB_PASSWORD 를 못 받았습니다}"

export SPRING_PROFILES_ACTIVE=prod

JAVA=$(command -v java)
exec "$JAVA" \
  -Xms128m -Xmx448m -Xss512k \
  -XX:MaxMetaspaceSize=192m \
  -XX:+UseSerialGC \
  -XX:+ExitOnOutOfMemoryError \
  -Duser.timezone=Asia/Seoul \
  -Dfile.encoding=UTF-8 \
  -jar "$JAR"
