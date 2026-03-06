#!/bin/sh
set -e

MINIO_HOST="${MINIO_HOST:-http://localhost:9000}"

minio "$@" &
MINIO_PID=$!

until mc alias set local "$MINIO_HOST" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
  echo "Waiting for MinIO..."
  sleep 2
done

if [ -n "$MINIO_APP_USER" ] && [ -n "$MINIO_APP_SECRET" ]; then
  mc admin user add local "$MINIO_APP_USER" "$MINIO_APP_SECRET" >/dev/null 2>&1 || true
  mc admin policy attach local readwrite --user "$MINIO_APP_USER" >/dev/null 2>&1 || true
fi

wait "$MINIO_PID"
