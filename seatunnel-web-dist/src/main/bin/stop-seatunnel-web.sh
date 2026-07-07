#!/usr/bin/env bash

set -euo pipefail

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEATUNNEL_WEB_HOME="$(cd "${BIN_DIR}/.." && pwd)"
PID_FILE="${SEATUNNEL_WEB_HOME}/seatunnel-web.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "SeaTunnel Web is not running: pid file not found."
  exit 0
fi

PID="$(cat "${PID_FILE}")"
if ! kill -0 "${PID}" >/dev/null 2>&1; then
  echo "SeaTunnel Web process ${PID} is not running."
  rm -f "${PID_FILE}"
  exit 0
fi

kill "${PID}"
for _ in {1..30}; do
  if ! kill -0 "${PID}" >/dev/null 2>&1; then
    rm -f "${PID_FILE}"
    echo "SeaTunnel Web stopped."
    exit 0
  fi
  sleep 1
done

echo "SeaTunnel Web did not stop within 30 seconds; sending SIGKILL."
kill -9 "${PID}" >/dev/null 2>&1 || true
rm -f "${PID_FILE}"
