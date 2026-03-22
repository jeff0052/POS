#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ROOT_DIR}/.env.prod"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ".env.prod not found. Copy .env.example to .env.prod and fill in real values first."
  exit 1
fi

cd "${ROOT_DIR}"

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build

echo ""
echo "POS production stack is starting."
echo "Check status with:"
echo "  docker compose --env-file .env.prod -f docker-compose.prod.yml ps"
echo ""
echo "App health:"
echo "  curl http://localhost:${APP_PORT:-80}/healthz"
