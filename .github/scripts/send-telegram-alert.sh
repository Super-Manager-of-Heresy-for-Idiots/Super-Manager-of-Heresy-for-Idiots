#!/usr/bin/env bash
set -euo pipefail

alert_status="${1:?alert status is required}"
stage_name="${STAGE_NAME:?STAGE_NAME is required}"
stage_result="${STAGE_RESULT:-}"

if [ -z "${TELEGRAM_BOT_TOKEN}" ] || [ "${TELEGRAM_BOT_TOKEN}" = "PUT_TELEGRAM_BOT_TOKEN_HERE" ] || \
   [ -z "${TELEGRAM_CHAT_ID}" ] || [ "${TELEGRAM_CHAT_ID}" = "PUT_TELEGRAM_CHAT_ID_HERE" ]; then
  echo "Telegram notification skipped: TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID is not configured."
  exit 0
fi

short_sha="${GITHUB_SHA::7}"
run_url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"

if [ "${alert_status}" = "success" ]; then
  message="$(cat <<EOF
Успешно
Repository: ${GITHUB_REPOSITORY}
Stage: ${stage_name}
Branch: ${GITHUB_REF_NAME}
Commit: ${short_sha}
Actor: ${GITHUB_ACTOR}
Run: ${run_url}
EOF
)"
else
  message="$(cat <<EOF
Ошибка
Repository: ${GITHUB_REPOSITORY}
Stage: ${stage_name}
Branch: ${GITHUB_REF_NAME}
Commit: ${short_sha}
Actor: ${GITHUB_ACTOR}
Error: stage ${stage_name} finished with ${stage_result}. Logs: ${run_url}
EOF
)"
fi

api_url="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage"
curl_args=(
  --fail
  --show-error
  --silent
  --output /dev/null
  --request POST
  "${api_url}"
  --data "chat_id=${TELEGRAM_CHAT_ID}"
  --data-urlencode "text=${message}"
  --data "disable_web_page_preview=true"
)

if [ -n "${TELEGRAM_THREAD_ID}" ]; then
  curl_args+=(--data "message_thread_id=${TELEGRAM_THREAD_ID}")
fi

curl "${curl_args[@]}" || echo "Telegram notification failed."
