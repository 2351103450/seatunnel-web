#!/usr/bin/env bash

set -euo pipefail

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEATUNNEL_WEB_HOME="$(cd "${BIN_DIR}/.." && pwd)"
PID_FILE="${SEATUNNEL_WEB_HOME}/seatunnel-web.pid"

if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" >/dev/null 2>&1; then
  echo "SeaTunnel Web is running with pid $(cat "${PID_FILE}")."
else
  echo "SeaTunnel Web is stopped."
fi
