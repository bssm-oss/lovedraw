#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DIR="$ROOT_DIR/release"
KEYSTORE_FILE="$KEYSTORE_DIR/lovedraw-upload.keystore"
PROPERTIES_FILE="$ROOT_DIR/keystore.properties"
ALIAS="${LOVEDRAW_RELEASE_KEY_ALIAS:-lovedraw}"

if [[ -f "$KEYSTORE_FILE" || -f "$PROPERTIES_FILE" ]]; then
  echo "Release keystore or keystore.properties already exists."
  echo "Refusing to overwrite: $KEYSTORE_FILE"
  echo "Refusing to overwrite: $PROPERTIES_FILE"
  exit 1
fi

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool not found. Install JDK 17 and try again."
  exit 1
fi

mkdir -p "$KEYSTORE_DIR"
chmod 700 "$KEYSTORE_DIR"

STORE_PASSWORD="$(LC_ALL=C tr -dc 'A-Za-z0-9_@%+=:,.~-' </dev/urandom | head -c 48)"
KEY_PASSWORD="$(LC_ALL=C tr -dc 'A-Za-z0-9_@%+=:,.~-' </dev/urandom | head -c 48)"

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -storetype JKS \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -alias "$ALIAS" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=lovedraw Upload, OU=lovedraw, O=lovedraw, L=Seoul, ST=Seoul, C=KR"

chmod 600 "$KEYSTORE_FILE"

cat > "$PROPERTIES_FILE" <<EOF
LOVEDRAW_RELEASE_STORE_FILE=release/lovedraw-upload.keystore
LOVEDRAW_RELEASE_STORE_PASSWORD=$STORE_PASSWORD
LOVEDRAW_RELEASE_KEY_ALIAS=$ALIAS
LOVEDRAW_RELEASE_KEY_PASSWORD=$KEY_PASSWORD
EOF

chmod 600 "$PROPERTIES_FILE"

echo "Created release upload keystore:"
echo "  $KEYSTORE_FILE"
echo "Created local signing properties:"
echo "  $PROPERTIES_FILE"
echo
echo "Keep both files private. Losing this upload key can block future app updates unless Play App Signing key reset is available."
echo "Next check:"
echo "  ./gradlew :app:bundleRelease"
