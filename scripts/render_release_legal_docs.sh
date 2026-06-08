#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT_DIR/build/release-legal"

load_properties() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  while IFS='=' read -r key value; do
    [[ -n "${key:-}" ]] || continue
    [[ "$key" =~ ^[[:space:]]*# ]] && continue
    key="$(printf '%s' "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    value="$(printf '%s' "${value:-}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    case "$key" in
      LOVEDRAW_PRIVACY_POLICY_URL|LOVEDRAW_ACCOUNT_DELETION_URL|LOVEDRAW_SUPPORT_EMAIL|LOVEDRAW_OPERATOR_NAME|LOVEDRAW_POLICY_EFFECTIVE_DATE|LOVEDRAW_DELETION_PROCESSING_PERIOD)
        if [[ -z "${!key:-}" && -n "$value" ]]; then
          export "$key=$value"
        fi
        ;;
    esac
  done < "$file"
}

is_placeholder() {
  local value="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
  [[ "$value" == *"your-"* ]] ||
    [[ "$value" == *"example."* ]] ||
    [[ "$value" == *"localhost"* ]] ||
    [[ "$value" == *"127.0.0.1"* ]] ||
    [[ "$value" == *"10.0.2.2"* ]] ||
    [[ "$value" == *"change-me"* ]] ||
    [[ "$value" == *"출시 전"* ]] ||
    [[ "$value" == *"입력 필요"* ]] ||
    [[ "$value" == *"확정 필요"* ]]
}

require_value() {
  local key="$1"
  local value="${!key:-}"
  if [[ -z "$value" ]] || is_placeholder "$value"; then
    echo "Missing production value: $key" >&2
    return 1
  fi
}

escape_sed() {
  printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'
}

render_doc() {
  local input="$1"
  local output="$2"
  local text
  text="$(cat "$input")"

  for key in \
    LOVEDRAW_PRIVACY_POLICY_URL \
    LOVEDRAW_ACCOUNT_DELETION_URL \
    LOVEDRAW_SUPPORT_EMAIL \
    LOVEDRAW_OPERATOR_NAME \
    LOVEDRAW_POLICY_EFFECTIVE_DATE \
    LOVEDRAW_DELETION_PROCESSING_PERIOD; do
    text="$(printf '%s' "$text" | sed "s/\`$key\`/$(escape_sed "${!key}")/g")"
    text="$(printf '%s' "$text" | sed "s/$key/$(escape_sed "${!key}")/g")"
  done

  if printf '%s' "$text" | grep -E 'LOVEDRAW_[A-Z_]+|출시 전|입력 필요|확정 필요' >/dev/null; then
    echo "Rendered document still contains release placeholders: $output" >&2
    return 1
  fi

  printf '%s\n' "$text" > "$output"
}

main() {
  load_properties "$ROOT_DIR/local.properties"
  load_properties "$ROOT_DIR/.env"

  local failures=0
  for key in \
    LOVEDRAW_PRIVACY_POLICY_URL \
    LOVEDRAW_ACCOUNT_DELETION_URL \
    LOVEDRAW_SUPPORT_EMAIL \
    LOVEDRAW_OPERATOR_NAME \
    LOVEDRAW_POLICY_EFFECTIVE_DATE \
    LOVEDRAW_DELETION_PROCESSING_PERIOD; do
    require_value "$key" || failures=1
  done

  if [[ "$failures" -ne 0 ]]; then
    echo "Set the missing values in local.properties, .env, or environment variables." >&2
    exit 1
  fi

  mkdir -p "$OUT_DIR"
  render_doc "$ROOT_DIR/docs/privacy-policy-ko.md" "$OUT_DIR/privacy-policy-ko.md"
  render_doc "$ROOT_DIR/docs/account-data-deletion-page-ko.md" "$OUT_DIR/account-data-deletion-page-ko.md"
  echo "Rendered release legal docs:"
  echo "- $OUT_DIR/privacy-policy-ko.md"
  echo "- $OUT_DIR/account-data-deletion-page-ko.md"
}

main "$@"
