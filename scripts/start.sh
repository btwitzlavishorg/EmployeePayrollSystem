#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-8080}"
PID_FILE="$ROOT_DIR/.payroll-server.pid"
JAR="$ROOT_DIR/target/payroll-system.jar"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Payroll server already running (PID $(cat "$PID_FILE"))."
  echo "Open http://localhost:${PORT}/"
  exit 0
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven (mvn) is required but was not found on PATH."
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required but was not found on PATH."
  exit 1
fi

echo "Building Employee Payroll System..."
mvn -q -DskipTests=false clean package

echo "Starting server on port ${PORT}..."
nohup java -Dserver.port="${PORT}" -jar "$JAR" > "$ROOT_DIR/payroll-server.log" 2>&1 &
echo $! > "$PID_FILE"

sleep 2
if kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Server started."
  echo "  URL : http://localhost:${PORT}/"
  echo "  Log : $ROOT_DIR/payroll-server.log"
  echo "  PID : $(cat "$PID_FILE")"
  echo "  Stop: ./scripts/stop.sh"
else
  echo "Server failed to start. See $ROOT_DIR/payroll-server.log"
  exit 1
fi
