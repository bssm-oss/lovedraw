# lovedraw 출시 전 실기기 증거 체크리스트

이 문서는 Google Play 제출 전에 로컬에 남겨야 하는 실기기/에뮬레이터 검증 증거 목록입니다. 증거 파일에는 계정, 방 코드, 화면 내용, 알림, 개인 이미지가 들어갈 수 있으므로 저장소에 커밋하지 않습니다.

기본 보관 위치:

```text
release-evidence/
```

다른 위치를 쓰려면 `LOVEDRAW_RELEASE_EVIDENCE_DIR` 환경변수로 지정합니다.

```bash
LOVEDRAW_RELEASE_EVIDENCE_DIR=/path/to/private/evidence ./scripts/verify_release_evidence.sh
```

## 필수 증거 파일

아래 이름으로 시작하는 비어 있지 않은 파일을 준비합니다. 확장자는 `mp4`, `mov`, `png`, `jpg`, `md`, `txt` 등 자유롭게 사용할 수 있습니다.

| 파일 이름 접두어 | 확인 내용 |
| --- | --- |
| `google-oauth` | Google 계정 선택, 로그인, 로그아웃, 재로그인 후 기존 기록 복구 |
| `permission-onboarding` | Android 시스템 권한 화면 전에 앱 내부 설명이 먼저 표시됨 |
| `notification-permission` | 알림 권한 허용/거부 흐름 |
| `overlay-permission` | 다른 앱 위 표시 권한 허용/거부 흐름 |
| `overlay-notification-controls` | 알림에서 그리기 시작, 끄기, 전체 지우기 동작 |
| `room-create-join` | 방 만들기, 코드 입장, 없는 코드, 꽉 찬 방 차단 |
| `invite-share` | QR, 초대 링크, Android 공유, 카카오톡/인스타그램 fallback |
| `two-device-overlay-sync` | 앱 밖 화면 위에 그린 선이 상대 기기에 실시간 표시됨 |
| `clear-and-fadeout` | 전체 지우기와 일정 시간 후 stroke fade-out |
| `privacy-mode-widget` | privacy mode에서 위젯/미리보기 민감 정보 숨김 |
| `privacy-legal-links` | 앱 설정의 개인정보처리방침, 계정 삭제 요청, 문의 링크 |

예시:

```text
release-evidence/google-oauth.mp4
release-evidence/permission-onboarding.png
release-evidence/two-device-overlay-sync.mov
```

## 검증 명령

```bash
./scripts/verify_release_evidence.sh
```

이 검사는 로컬 증거 파일 존재 여부만 확인합니다. 영상 내용의 정확성은 사람이 직접 확인해야 합니다.

## 보안 원칙

- 증거 파일은 `.gitignore`와 `scripts/check_sensitive_files.sh`에서 차단합니다.
- 방 코드, Google 계정, 개인 사진, 알림 내용이 보이면 외부 공유용 영상에서는 가립니다.
- Play 심사용 제출 영상은 실제 사용자가 이해할 수 있는 흐름으로 촬영합니다.
- 정책/개인정보 문서와 앱 내부 권한 안내가 서로 다른 말을 하지 않는지 함께 확인합니다.
