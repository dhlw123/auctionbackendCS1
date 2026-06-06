#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SERVER_PID=""
SERVER_PORT=8080

cleanup() {
    if [ -n "${SERVER_PID:-}" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
        echo ""
        echo "=== Stopping server (pid=$SERVER_PID) ==="
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

wait_for_server() {
    local max_attempts=60
    local attempt=0
    echo "=== Waiting for server on port $SERVER_PORT ==="
    while [ $attempt -lt $max_attempts ]; do
        if curl -sf "http://localhost:${SERVER_PORT}/items/all" >/dev/null 2>&1; then
            echo "=== Server is ready ==="
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
        echo -n "."
    done
    echo ""
    echo "ERROR: Server did not start within ${max_attempts}s"
    return 1
}

# ── Option 1: Start server ourselves ─────────────────────────────

start_server() {
    echo "=== Starting Spring Boot server ==="
    cd "$PROJECT_DIR"
    ./gradlew bootRun &
    SERVER_PID=$!
    wait_for_server
}

# ── Option 2: Assume server is already running ────────────────────

external_server() {
    if curl -sf "http://localhost:${SERVER_PORT}/items/all" >/dev/null 2>&1; then
        echo "=== Using external server on port $SERVER_PORT ==="
    else
        echo "=== No external server found, starting local server ==="
        start_server
    fi
}

# ── Seed admin user ────────────────────────────────────────────────

seed_admin() {
    local admin_user="${ADMIN_USERNAME:-admin}"
    local admin_pass="${ADMIN_PASSWORD:-admin}"
    echo "=== Ensuring admin user exists (${admin_user}/${admin_pass}) ==="
    if curl -sf -X POST "http://localhost:${SERVER_PORT}/login" \
         -H "Content-Type: application/json" \
         -d "{\"username\":\"${admin_user}\",\"password\":\"${admin_pass}\"}" >/dev/null 2>&1; then
        echo "=== Admin user already exists ==="
    else
        echo "=== Registering admin user ==="
        curl -s -X POST "http://localhost:${SERVER_PORT}/register" \
             -H "Content-Type: application/json" \
             -d "{\"username\":\"${admin_user}\",\"displayName\":\"Administrator\",\"password\":\"${admin_pass}\"}" >/dev/null
        if curl -sf -X POST "http://localhost:${SERVER_PORT}/login" \
             -H "Content-Type: application/json" \
             -d "{\"username\":\"${admin_user}\",\"password\":\"${admin_pass}\"}" >/dev/null 2>&1; then
            echo "=== Admin user created successfully ==="
        else
            echo "ERROR: Failed to create admin user"
            return 1
        fi
    fi
}

# ── Main ──────────────────────────────────────────────────────────

echo "=== Auction API Test Suite ==="

if [ "${START_SERVER:-1}" = "1" ] && [ "${EXTERNAL_SERVER:-0}" != "1" ]; then
    start_server
else
    external_server
fi

seed_admin

VENV_DIR="$PROJECT_DIR/.venv_api_tests"
if [ ! -f "$VENV_DIR/bin/python3" ]; then
    python3 -m venv "$VENV_DIR"
    "$VENV_DIR/bin/pip" install -q pytest requests
fi

echo ""
echo "=== Running tests ==="
cd "$PROJECT_DIR"
"$VENV_DIR/bin/python3" -m pytest tests_api/ -v --tb=short "$@"
EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "=== All tests passed ==="
else
    echo "=== Tests failed with exit code $EXIT_CODE ==="
fi
exit $EXIT_CODE
