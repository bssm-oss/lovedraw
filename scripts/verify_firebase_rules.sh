#!/usr/bin/env bash
set -euo pipefail

BASE="${FIREBASE_DATABASE_EMULATOR_URL:-http://127.0.0.1:9000}"
AUTH_BASE="${FIREBASE_AUTH_EMULATOR_URL:-http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1}"
API_KEY="${FIREBASE_WEB_API_KEY:-$(jq -r '.client[0].api_key[0].current_key' app/google-services.json)}"
DATABASE_NAMESPACE="${FIREBASE_DATABASE_NAMESPACE:-$(jq -r '.project_info.firebase_url' app/google-services.json | sed -E 's#https://([^./]+).*#\1#')}"

create_auth_user() {
  curl -sS "${AUTH_BASE}/accounts:signUp?key=${API_KEY}" \
    -H "Content-Type: application/json" \
    --data-binary '{"returnSecureToken":true}' |
    jq -r '[.localId, .idToken] | @tsv'
}

request_code() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local url="${BASE}${path}.json?ns=${DATABASE_NAMESPACE}"

  if [[ -n "$token" ]]; then
    url="${url}&auth=${token}"
  fi

  if [[ -n "$body" ]]; then
    curl -sS -o /tmp/couple-canvas-rules-response.json -w "%{http_code}" \
      -X "$method" "$url" -H "Content-Type: application/json" --data-binary "$body"
  else
    curl -sS -o /tmp/couple-canvas-rules-response.json -w "%{http_code}" \
      -X "$method" "$url"
  fi
}

expect_code() {
  local expected="$1"
  local method="$2"
  local path="$3"
  local token="${4:-}"
  local body="${5:-}"
  local actual
  actual="$(request_code "$method" "$path" "$token" "$body")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected HTTP $expected for $method $path, got $actual" >&2
    cat /tmp/couple-canvas-rules-response.json >&2 || true
    exit 1
  fi
}

IFS=$'\t' read -r HOST_UID HOST_TOKEN < <(create_auth_user)
IFS=$'\t' read -r GUEST_UID GUEST_TOKEN < <(create_auth_user)
IFS=$'\t' read -r OUTSIDER_UID OUTSIDER_TOKEN < <(create_auth_user)
ROOM_ID="rules-room-$(date +%s)-$RANDOM"
ROOM_CODE="ABCD$(printf '%02d' $((RANDOM % 8 + 22)))"
NOW=1800000000000

expect_code 200 PUT "/users/$HOST_UID" "$HOST_TOKEN" "{\"uid\":\"$HOST_UID\",\"displayName\":\"Host\",\"createdAt\":1800000000000,\"lastSeen\":1800000000000}"
expect_code 401 PUT "/users/$HOST_UID" "$OUTSIDER_TOKEN" "{\"uid\":\"$HOST_UID\",\"lastSeen\":1800000000000}"

CREATE_PAYLOAD="$(cat <<JSON
{
  "rooms/$ROOM_ID": {
    "roomId": "$ROOM_ID",
    "roomCode": "$ROOM_CODE",
    "title": "Rules Test Room",
    "createdAt": $NOW,
    "updatedAt": $NOW,
    "status": "waiting",
    "hostUid": "$HOST_UID",
    "activeUserCount": 1,
    "members": {
      "$HOST_UID": true
    },
    "users": {
      "$HOST_UID": {
        "uid": "$HOST_UID",
        "role": "host",
        "joinedAt": $NOW,
        "online": true,
        "lastSeen": $NOW
      }
    }
  },
  "userRooms/$HOST_UID/$ROOM_ID": true
}
JSON
)"
expect_code 200 PATCH "/" "$HOST_TOKEN" "$CREATE_PAYLOAD"
expect_code 401 PUT "/roomCodes/EVIL99" "$OUTSIDER_TOKEN" "\"fake-room\""
expect_code 200 PUT "/roomCodes/$ROOM_CODE" "$HOST_TOKEN" "\"$ROOM_ID\""
expect_code 200 GET "/rooms/$ROOM_ID" "$HOST_TOKEN"
expect_code 401 GET "/rooms/$ROOM_ID" "$OUTSIDER_TOKEN"

JOIN_PAYLOAD="$(cat <<JSON
{
  "roomId": "$ROOM_ID",
  "roomCode": "$ROOM_CODE",
  "title": "Rules Test Room",
  "createdAt": $NOW,
  "updatedAt": $((NOW + 1)),
  "status": "active",
  "hostUid": "$HOST_UID",
  "guestUid": "$GUEST_UID",
  "activeUserCount": 2,
  "members": {
    "$HOST_UID": true,
    "$GUEST_UID": true
  },
  "users": {
    "$HOST_UID": {
      "uid": "$HOST_UID",
      "role": "host",
      "joinedAt": $NOW,
      "online": true,
      "lastSeen": $NOW
    }
  }
}
JSON
)"
expect_code 200 PUT "/rooms/$ROOM_ID" "$GUEST_TOKEN" "$JOIN_PAYLOAD"
expect_code 200 PUT "/userRooms/$GUEST_UID/$ROOM_ID" "$GUEST_TOKEN" 'true'

NOTE_PAYLOAD="$(cat <<JSON
{
  "noteId": "note-1",
  "authorUid": "$HOST_UID",
  "message": "규칙 테스트 노트",
  "createdAt": $((NOW + 2)),
  "updatedAt": $((NOW + 2)),
  "isPinned": false,
  "isRead": false
}
JSON
)"
expect_code 200 PUT "/rooms/$ROOM_ID/notes/note-1" "$HOST_TOKEN" "$NOTE_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/notes/note-2" "$OUTSIDER_TOKEN" "$NOTE_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/adminOverride" "$HOST_TOKEN" '"not allowed"'
expect_code 401 DELETE "/rooms/$ROOM_ID/notes/note-1" "$GUEST_TOKEN"

SPOOFED_NOTE_PAYLOAD="$(cat <<JSON
{
  "noteId": "note-2",
  "authorUid": "$GUEST_UID",
  "message": "다른 사람 이름으로 쓰려는 노트",
  "createdAt": $((NOW + 2)),
  "updatedAt": $((NOW + 2)),
  "isPinned": false,
  "isRead": false
}
JSON
)"
expect_code 401 PUT "/rooms/$ROOM_ID/notes/note-2" "$HOST_TOKEN" "$SPOOFED_NOTE_PAYLOAD"

HOST_SPARK_PAYLOAD="{\"uid\":\"$HOST_UID\",\"answer\":\"오늘도 고마웠어\",\"createdAt\":$((NOW + 2))}"
SPOOFED_SPARK_PAYLOAD="{\"uid\":\"$GUEST_UID\",\"answer\":\"상대방 답변을 대신 쓰기\",\"createdAt\":$((NOW + 2))}"
expect_code 200 PUT "/rooms/$ROOM_ID/dailySparks/2026-05-28" "$HOST_TOKEN" '{"sparkId":"2026-05-28","question":"오늘 고마웠던 순간은?","dateKey":"2026-05-28"}'
expect_code 200 PUT "/rooms/$ROOM_ID/dailySparks/2026-05-28/answers/$HOST_UID" "$HOST_TOKEN" "$HOST_SPARK_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/dailySparks/2026-05-28/answers/$GUEST_UID" "$HOST_TOKEN" "$SPOOFED_SPARK_PAYLOAD"

HOST_STROKE_PAYLOAD="$(cat <<JSON
{
  "strokeId": "stroke-1",
  "ownerUid": "$HOST_UID",
  "color": "#FF7A9A",
  "width": 8,
  "eraser": false,
  "createdAt": $((NOW + 2))
}
JSON
)"
SPOOFED_STROKE_PAYLOAD="$(echo "$HOST_STROKE_PAYLOAD" | jq --arg uid "$GUEST_UID" '.ownerUid = $uid')"
expect_code 200 PUT "/rooms/$ROOM_ID/strokes/stroke-1" "$HOST_TOKEN" "$HOST_STROKE_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/strokes/stroke-2" "$HOST_TOKEN" "$SPOOFED_STROKE_PAYLOAD"

HOST_DATE_PAYLOAD="$(cat <<JSON
{
  "planId": "date-1",
  "title": "규칙 테스트 데이트",
  "description": "안전한 활동 플랜",
  "vibe": "Cozy",
  "mode": "nearby",
  "tone": "safe",
  "status": "saved",
  "createdByUid": "$HOST_UID",
  "createdAt": $((NOW + 2)),
  "updatedAt": $((NOW + 2))
}
JSON
)"
SPOOFED_DATE_PAYLOAD="$(echo "$HOST_DATE_PAYLOAD" | jq --arg uid "$GUEST_UID" '.planId = "date-2" | .createdByUid = $uid')"
expect_code 200 PUT "/rooms/$ROOM_ID/datePlans/date-1" "$HOST_TOKEN" "$HOST_DATE_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/datePlans/date-2" "$HOST_TOKEN" "$SPOOFED_DATE_PAYLOAD"
expect_code 401 DELETE "/rooms/$ROOM_ID/datePlans/date-1" "$GUEST_TOKEN"

HOST_MEMORY_PAYLOAD="$(cat <<JSON
{
  "memoryId": "memory-1",
  "title": "규칙 테스트 추억",
  "date": $((NOW + 2)),
  "createdByUid": "$HOST_UID",
  "createdAt": $((NOW + 2)),
  "updatedAt": $((NOW + 2))
}
JSON
)"
SPOOFED_MEMORY_PAYLOAD="$(echo "$HOST_MEMORY_PAYLOAD" | jq --arg uid "$GUEST_UID" '.memoryId = "memory-2" | .createdByUid = $uid')"
expect_code 200 PUT "/rooms/$ROOM_ID/memories/memory-1" "$HOST_TOKEN" "$HOST_MEMORY_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/memories/memory-2" "$HOST_TOKEN" "$SPOOFED_MEMORY_PAYLOAD"
expect_code 401 DELETE "/rooms/$ROOM_ID/memories/memory-1" "$GUEST_TOKEN"
UPDATED_MEMORY_PAYLOAD="$(echo "$HOST_MEMORY_PAYLOAD" | jq --arg path "rooms/$ROOM_ID/uploads/$HOST_UID/memories/memory-1/1.jpg" '.imageUrls = ["https://example.invalid/1.jpg"] | .storagePaths = [$path] | .updatedAt = 1800000000003')"
expect_code 200 PUT "/rooms/$ROOM_ID/memories/memory-1" "$HOST_TOKEN" "$UPDATED_MEMORY_PAYLOAD"

HOST_BUCKET_PAYLOAD="$(cat <<JSON
{
  "itemId": "bucket-1",
  "title": "규칙 테스트 버킷",
  "status": "wish",
  "createdByUid": "$HOST_UID",
  "createdAt": $((NOW + 2)),
  "updatedAt": $((NOW + 2))
}
JSON
)"
SPOOFED_BUCKET_PAYLOAD="$(echo "$HOST_BUCKET_PAYLOAD" | jq --arg uid "$GUEST_UID" '.itemId = "bucket-2" | .createdByUid = $uid')"
expect_code 200 PUT "/rooms/$ROOM_ID/bucketList/bucket-1" "$HOST_TOKEN" "$HOST_BUCKET_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/bucketList/bucket-2" "$HOST_TOKEN" "$SPOOFED_BUCKET_PAYLOAD"
expect_code 401 DELETE "/rooms/$ROOM_ID/bucketList/bucket-1" "$GUEST_TOKEN"

HOST_QUIZ_PAYLOAD="$(cat <<JSON
{
  "answerId": "taste-1-$HOST_UID",
  "questionId": "taste-1",
  "uid": "$HOST_UID",
  "answer": "산책",
  "createdAt": $((NOW + 2))
}
JSON
)"
SPOOFED_QUIZ_PAYLOAD="$(echo "$HOST_QUIZ_PAYLOAD" | jq --arg uid "$GUEST_UID" '.answerId = ("taste-1-" + $uid) | .uid = $uid')"
expect_code 200 PUT "/rooms/$ROOM_ID/quizAnswers/taste-1-$HOST_UID" "$HOST_TOKEN" "$HOST_QUIZ_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/quizAnswers/taste-1-$GUEST_UID" "$HOST_TOKEN" "$SPOOFED_QUIZ_PAYLOAD"

HOST_LOCATION_PAYLOAD="$(cat <<JSON
{
  "uid": "$HOST_UID",
  "enabled": true,
  "consentedUids": {
    "$HOST_UID": true
  },
  "isApproximateOnly": true
}
JSON
)"
SPOOFED_LOCATION_PAYLOAD="$(echo "$HOST_LOCATION_PAYLOAD" | jq --arg uid "$GUEST_UID" '.uid = $uid')"
expect_code 200 PUT "/rooms/$ROOM_ID/locationShares/$HOST_UID" "$HOST_TOKEN" "$HOST_LOCATION_PAYLOAD"
expect_code 401 PUT "/rooms/$ROOM_ID/locationShares/$GUEST_UID" "$HOST_TOKEN" "$SPOOFED_LOCATION_PAYLOAD"
HOST_LOCATION_WITH_COORDS="$(echo "$HOST_LOCATION_PAYLOAD" | jq '.latitude = 37.5665 | .longitude = 126.9780 | .accuracyMeters = 24 | .isApproximateOnly = false | .consentedUids["'"$GUEST_UID"'"] = true')"
GUEST_LOCATION_CONSENT="$(cat <<JSON
{
  "uid": "$GUEST_UID",
  "enabled": true,
  "consentedUids": {
    "$GUEST_UID": true
  },
  "isApproximateOnly": true
}
JSON
)"
expect_code 401 PUT "/rooms/$ROOM_ID/locationShares/$HOST_UID" "$HOST_TOKEN" "$HOST_LOCATION_WITH_COORDS"
expect_code 200 PUT "/rooms/$ROOM_ID/locationShares/$GUEST_UID" "$GUEST_TOKEN" "$GUEST_LOCATION_CONSENT"
expect_code 200 PUT "/rooms/$ROOM_ID/locationShares/$HOST_UID" "$HOST_TOKEN" "$HOST_LOCATION_WITH_COORDS"

FULL_JOIN_PAYLOAD="$(cat <<JSON
{
  "roomId": "$ROOM_ID",
  "roomCode": "$ROOM_CODE",
  "title": "Rules Test Room",
  "createdAt": $NOW,
  "updatedAt": $((NOW + 2)),
  "status": "active",
  "hostUid": "$HOST_UID",
  "guestUid": "$OUTSIDER_UID",
  "activeUserCount": 3,
  "members": {
    "$HOST_UID": true,
    "$GUEST_UID": true,
    "$OUTSIDER_UID": true
  }
}
JSON
)"
expect_code 401 PUT "/rooms/$ROOM_ID" "$OUTSIDER_TOKEN" "$FULL_JOIN_PAYLOAD"
expect_code 401 PUT "/roomCodes/$ROOM_CODE" "$OUTSIDER_TOKEN" '"other-room"'

CLOSE_PAYLOAD="$(cat <<JSON
{
  "rooms/$ROOM_ID/status": "closed",
  "rooms/$ROOM_ID/activeUserCount": 0,
  "rooms/$ROOM_ID/updatedAt": $((NOW + 3)),
  "roomCodes/$ROOM_CODE": null
}
JSON
)"
expect_code 200 PATCH "/" "$HOST_TOKEN" "$CLOSE_PAYLOAD"
expect_code 200 DELETE "/userRooms/$HOST_UID/$ROOM_ID" "$HOST_TOKEN"

echo "Firebase Realtime Database production rules smoke test passed."
