#!/usr/bin/env bash
# Wait for ExpenseGuard Backend Health Endpoint Readiness Script

HEALTH_URL="${1:-http://localhost:8081/api/v1/health}"
MAX_RETRIES=${2:-30}
SLEEP_INTERVAL=${3:-2}

echo "Waiting for ExpenseGuard Backend health check at ${HEALTH_URL}..."

for ((i=1; i<=MAX_RETRIES; i++)); do
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${HEALTH_URL}" || echo "000")
  
  if [ "${HTTP_STATUS}" -eq 200 ]; then
    echo "Health check SUCCESS: Backend is UP and responding with HTTP 200 OK (Attempt ${i}/${MAX_RETRIES})"
    exit 0
  fi
  
  echo "Backend not ready yet (HTTP ${HTTP_STATUS}). Retrying in ${SLEEP_INTERVAL}s... (${i}/${MAX_RETRIES})"
  sleep "${SLEEP_INTERVAL}"
done

echo "ERROR: Health check TIMEOUT after ${MAX_RETRIES} attempts. Backend failed to respond with HTTP 200 OK."
exit 1
