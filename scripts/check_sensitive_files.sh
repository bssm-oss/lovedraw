#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FORBIDDEN_TRACKED_FILES="$(
  git ls-files \
    firebase-debug.log 'firebase-debug.log*' \
    database-debug.log 'database-debug.log*' \
    '*.log' \
    '.env' '.env.*' \
    'app/google-services.json' 'app/src/*/google-services.json' '**/google-services.json' '**/google-services.json.*' \
    '.firebaserc' \
    'keystore.properties' '*.keystore' '*.jks' '*.p12' '*.pem' '*.key' \
    'serviceAccount*.json' '*-service-account.json' 'firebase-adminsdk*.json' \
  | grep -Ev '(^|/)(\.env\.example|\.firebaserc\.example|google-services\.example\.json|keystore\.properties\.example)$' || true
)"

if [[ -n "$FORBIDDEN_TRACKED_FILES" ]]; then
  echo "Forbidden sensitive files are tracked by git:" >&2
  echo "$FORBIDDEN_TRACKED_FILES" >&2
  exit 1
fi

SECRET_MATCHES="$(
  git grep -n -I -E \
    '(AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|firebase-adminsdk|private_key_id|private_key)' \
    -- \
    ':!app/google-services.example.json' \
    ':!.env.example' \
    ':!.gitignore' \
    ':!docs/**' \
    ':!README.md' \
    ':!scripts/check_sensitive_files.sh' \
  || true
)"

if [[ -n "$SECRET_MATCHES" ]]; then
  echo "Potential secret-like content was found in tracked files:" >&2
  echo "$SECRET_MATCHES" >&2
  exit 1
fi

echo "Sensitive file check passed."
