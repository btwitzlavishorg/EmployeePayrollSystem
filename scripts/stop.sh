#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$ROOT_DIR/.payroll-server.pid"
PORT="${PORT:-8080}"

stopped=false

if [[ -f "$PID_FILE" ]]; then
  PID="$(cat "$PID_FILE")"
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID" 2>/dev/null || true
    sleep 1
    if kill -0 "$PID" 2>/dev/null; then
      kill -9 "$PID" 2>/dev/null || true
    fi
    echo "Stopped payroll server (PID $PID)."
    stopped=true
  fi
  rm -f "$PID_FILE"
fi

# Fallback: free the port if something is still bound
if command -v fuser >/dev/null 2>&1; then
  if fuser "${PORT}/tcp" >/dev/null 2>&1; then
    fuser -k "${PORT}/tcp" >/dev/null 2>&1 || true
    echo "Freed port ${PORT}."
    stopped=true
  fi
fi

if [[ "$stopped" = false ]]; then
  echo "No running payroll server found."
fi
