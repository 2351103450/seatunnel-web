#!/usr/bin/env bash

set -euo pipefail

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEATUNNEL_WEB_HOME="$(cd "${BIN_DIR}/.." && pwd)"
CONF_DIR="${SEATUNNEL_WEB_HOME}/conf"
LOG_DIR="${SEATUNNEL_WEB_HOME}/logs"
PID_FILE="${SEATUNNEL_WEB_HOME}/seatunnel-web.pid"
JAVA_BIN="${JAVA_HOME:-}/bin/java"

if [[ ! -x "${JAVA_BIN}" ]]; then
  JAVA_BIN="$(command -v java)"
fi

if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" >/dev/null 2>&1; then
  echo "SeaTunnel Web is already running with pid $(cat "${PID_FILE}")."
  exit 0
fi

mkdir -p "${LOG_DIR}"
JAVA_OPTS="${JAVA_OPTS:-}"
APP_OPTS="${APP_OPTS:-}"

nohup "${JAVA_BIN}" ${JAVA_OPTS} \
  -Dseatunnel.web.home="${SEATUNNEL_WEB_HOME}" \
  -Dlogging.config="${CONF_DIR}/logback-spring.xml" \
  -jar "${SEATUNNEL_WEB_HOME}/libs/seatunnel-web-api.jar" \
  --spring.config.location="${CONF_DIR}/application.yml" \
  ${APP_OPTS} > "${LOG_DIR}/seatunnel-web.out" 2>&1 &

echo $! > "${PID_FILE}"
echo "SeaTunnel Web started with pid $(cat "${PID_FILE}")."
