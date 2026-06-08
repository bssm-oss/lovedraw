#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_DIR="${LOVEDRAW_RELEASE_EVIDENCE_DIR:-$ROOT_DIR/release-evidence}"

REQUIRED_ARTIFACTS=(
  "google-oauth|Google account chooser, sign-in, sign-out, and re-login record restore"
  "permission-onboarding|In-app permission explanation before Android system permission surfaces"
  "notification-permission|Notification permission allow and deny behavior"
  "overlay-permission|Draw over other apps permission allow and deny behavior"
  "overlay-notification-controls|Persistent notification actions for start, stop, and clear"
  "room-create-join|Room creation, invite code entry, full-room rejection, and invalid-code handling"
  "invite-share|QR, invite link, Android share sheet, Kakao, and Instagram fallback behavior"
  "two-device-overlay-sync|Two-device or two-emulator overlay drawing sync while outside the app"
  "clear-and-fadeout|Clear-all action and timed highlight fade-out behavior"
  "privacy-mode-widget|Privacy mode hiding sensitive widget or preview content"
  "privacy-legal-links|Settings screen privacy policy, account deletion, and support links"
)

artifact_exists() {
  local slug="$1"
  [[ -d "$EVIDENCE_DIR" ]] || return 1

  find "$EVIDENCE_DIR" \
    -maxdepth 1 \
    -type f \
    -size +0c \
    \( -name "$slug.*" -o -name "$slug-*" -o -name "${slug}_*" \) \
    -print \
    -quit |
    grep -q .
}

main() {
  if [[ ! -d "$EVIDENCE_DIR" ]]; then
    echo "Missing release evidence directory: $EVIDENCE_DIR" >&2
    echo "Create it locally and add proof files before release. This directory is ignored by git." >&2
    exit 1
  fi

  local missing=()
  local item slug description
  for item in "${REQUIRED_ARTIFACTS[@]}"; do
    slug="${item%%|*}"
    description="${item#*|}"
    if ! artifact_exists "$slug"; then
      missing+=("$slug|$description")
    fi
  done

  if [[ "${#missing[@]}" -gt 0 ]]; then
    echo "Missing release evidence artifacts in: $EVIDENCE_DIR" >&2
    echo >&2
    for item in "${missing[@]}"; do
      slug="${item%%|*}"
      description="${item#*|}"
      echo "- $slug: $description" >&2
      echo "  Example filename: $slug.mp4 or $slug.png" >&2
    done
    echo >&2
    echo "Keep these files local. Do not commit screenshots or videos with personal data." >&2
    exit 1
  fi

  echo "Release evidence check passed:"
  echo "- $EVIDENCE_DIR"
}

main "$@"
