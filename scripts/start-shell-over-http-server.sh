#!/usr/bin/env bash
set -e

PORT=${1:-8080}
TIMEOUT=${2:-60}
LOG_FILE="/tmp/shell-over-http-server.log"

./gradlew :shell-over-http:compileKotlin

echo "Starting shell-over-http server on port $PORT..."

: > "$LOG_FILE"  # Clear the log file
./gradlew :shell-over-http:run --args="--port=$PORT" >> "$LOG_FILE" 2>&1 &
SERVER_PID=$!

# Wait for server to be ready.
if timeout "$TIMEOUT" grep -q -m 1 "Shell server running" <(tail -f "$LOG_FILE"); then
    echo "Server is ready on http://localhost:$PORT"
    echo "To stop: pkill -f 'shell-over-http'"
else
    echo "ERROR: Server failed to start within ${TIMEOUT}s"
    echo "--- Log output ---"
    cat "$LOG_FILE"
    echo "------------------"
    kill $SERVER_PID 2>/dev/null || true
    exit 1
fi
