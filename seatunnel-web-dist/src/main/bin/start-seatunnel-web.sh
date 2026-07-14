#!/usr/bin/env bash

set -euo pipefail

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEATUNNEL_WEB_HOME="$(cd "${BIN_DIR}/.." && pwd)"

LOG_DIR="${SEATUNNEL_WEB_HOME}/logs"
PID_FILE="${SEATUNNEL_WEB_HOME}/seatunnel-web.pid"
RUN_SCRIPT="${BIN_DIR}/run-seatunnel-web.sh"

if [[ -f "${PID_FILE}" ]]; then
  PID="$(cat "${PID_FILE}")"

  if kill -0 "${PID}" >/dev/null 2>&1; then
    echo "SeaTunnel Web is already running with pid ${PID}."
    exit 0
  fi

  rm -f "${PID_FILE}"
fi

mkdir -p "${LOG_DIR}"

nohup "${RUN_SCRIPT}" \
  > "${LOG_DIR}/seatunnel-web.out" \
  2>&1 &

PID=$!
echo "${PID}" > "${PID_FILE}"

sleep 1

if ! kill -0 "${PID}" >/dev/null 2>&1; then
  echo "SeaTunnel Web failed to start. Check ${LOG_DIR}/seatunnel-web.out." >&2
  rm -f "${PID_FILE}"
  exit 1
fi

echo "SeaTunnel Web started with pid ${PID}."