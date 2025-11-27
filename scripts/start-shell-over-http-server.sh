#!/usr/bin/env bash
set -e

PORT=${1:-8080}
TIMEOUT=${2:-30}

echo "Starting shell-over-http server on port $PORT..."

./gradlew :shell-over-http:run --args="--port=$PORT" > /dev/null 2>&1 &
SERVER_PID=$!

# Wait for server to be ready
SECONDS=0
until curl -s "http://localhost:$PORT/run?cmd=true" > /dev/null 2>&1; do
    if [ $SECONDS -ge "$TIMEOUT" ]; then
        echo "ERROR: Server failed to start within ${TIMEOUT}s"
        kill $SERVER_PID 2>/dev/null || true
        exit 1
    fi
    sleep 0.5
done

echo "Shell-over-http server is ready on http://localhost:$PORT"
echo "To stop: kill $SERVER_PID (or kill all Gradle: pkill -f 'shell-over-http')"

