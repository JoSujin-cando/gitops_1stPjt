#!/usr/bin/env bash
set -euo pipefail

# --- 0) 프로젝트 루트 고정 ---
APP_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_ROOT"

# --- 1) servlet-api.jar 경로 (기존 방식 유지) ---
SERVLET_API="$HOME/tomcatnew/tomcat/lib/servlet-api.jar"
if [ ! -f "$SERVLET_API" ]; then
  if [ -n "${CATALINA_HOME:-}" ] && [ -f "$CATALINA_HOME/lib/servlet-api.jar" ]; then
    SERVLET_API="$CATALINA_HOME/lib/servlet-api.jar"
  else
    echo "[ERROR] servlet-api.jar를 찾을 수 없습니다."
    echo "        \$HOME/tomcatnew/tomcat/lib/servlet-api.jar 또는 \$CATALINA_HOME/lib/servlet-api.jar 확인!"
    exit 1
  fi
fi

# --- 2) 디렉토리/라이브러리 준비 확인 (기존 유지) ---
mkdir -p ROOT/WEB-INF/classes
if [ ! -d "ROOT/WEB-INF/lib" ]; then
  echo "[ERROR] WEB-INF/lib 디렉토리가 없습니다."
  exit 1
fi

# --- 3) 클래스패스 구성: servlet-api + ROOT/WEB-INF/lib 내 모든 JAR 포함 ---
LIB_JARS=$(find ROOT/WEB-INF/lib -name '*.jar' -printf '%p:' | sed 's/:$//')
CP="$SERVLET_API:$LIB_JARS"

# --- 4) 소스 목록 생성 ---
mapfile -t SRC_FILES < <(find src -type f -name "*.java" | sort)
if [ "${#SRC_FILES[@]}" -eq 0 ]; then
  echo "[ERROR] src/*.java 파일을 찾지 못했습니다."
  exit 1
fi

echo "[INFO] javac 시작..."
javac -encoding UTF-8 -cp "$CP" -d ROOT/WEB-INF/classes "${SRC_FILES[@]}"

echo "[OK] 컴파일 완료!"
echo
echo "[INFO] 산출물 확인:"
ls -la ROOT/WEB-INF/classes || true
echo

# (선택) 패키지 내 클래스 출력
if [ -d "ROOT/WEB-INF/classes/com/example/servlet" ]; then
  echo "[INFO] com/example/servlet 패키지 산출물:"
  ls -la ROOT/WEB-INF/classes/com/example/servlet || true
fi

docker restart tc
