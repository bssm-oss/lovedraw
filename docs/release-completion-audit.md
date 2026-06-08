# lovedraw 출시 완료 감사표

이 문서는 “완성”이라고 말하기 전에 코드, 테스트, 문서, 실기기 증거가 같은 기준을 보고 있는지 확인하기 위한 감사표입니다. 저장소 안에서는 자동 검증 가능한 항목만 완료로 고정하고, 실제 계정/기기/운영 URL이 필요한 항목은 외부 입력이 필요한 항목으로 분리합니다.

## 코드에서 완료한 항목

| 영역 | 완료 기준 | 고정 장치 |
| --- | --- | --- |
| 첫 실행 권한 안내 | Android 권한 화면 전에 알림과 다른 앱 위 표시가 왜 필요한지 앱 내부 문구로 설명 | `PermissionOnboardingCopyTest`, `ReleaseDocumentationConsistencyTest` |
| 홈 빠른 시작 | 방이 연결된 뒤 앱 밖 화면 위에 그릴 수 있음을 짧은 사용자 문구로 안내 | `OverlayQuickStartCopyTest` |
| 연결 상태 | 연결됨, 상대 대기 중, 재연결 중, 종료됨 상태가 사용자에게 명확히 표시 | `RoomConnectionStatusTest` |
| 오버레이 드로잉 품질 | 빠른 손 움직임도 끊기지 않게 보간하고 round cap/marker/fade 스타일 유지 | `StrokeInputInterpolatorTest`, `StrokePointInterpolatorTest`, `StrokeExpiryTest` |
| 초대 공유 | 초대 링크, QR, Android 공유, 카카오톡, 인스타그램 fallback 제공 | `WaitingInviteCopyTest`, `ShareVisibilityManifestTest` |
| 방과 Firebase 동기화 | 방 만들기, 코드 입장, 동시 입장 방지, 실시간 stroke 전송 | `RoomCodeGeneratorTest`, Firebase repository tests or emulator evidence |
| privacy mode와 위젯 | 민감한 위젯/미리보기 내용 숨김 | `WidgetSnapshotFactoryTest`, `privacy-mode-widget` 증거 |
| 권한/민감 정보 선언 | 앱이 쓰는 권한만 Manifest에 남기고 몰래 추적/캡처 권한 차단 | `ManifestPrivacyPolicyTest`, `scripts/check_sensitive_files.sh` |
| 법적 링크 | 개인정보처리방침, 계정/데이터 삭제, 문의 링크가 비어 있거나 placeholder면 release readiness 실패 | `ReleaseLegalLinksTest`, `:app:verifyReleaseReadiness` |
| 출시 문서 일관성 | 앱 내부 권한 설명, Foreground Service 제출 문구, Data safety 초안이 서로 같은 말을 함 | `ReleaseDocumentationConsistencyTest`, `scripts/render_release_legal_docs.sh` |
| 실기기 증거 목록 | Google Play 제출 전 필요한 영상/스크린샷 목록이 스크립트와 문서에서 일치 | `ReleaseEvidenceChecklistTest`, `scripts/verify_release_evidence.sh` |

## 외부 입력이 필요한 항목

아래 항목은 저장소에서 대체값으로 통과시키면 안 됩니다. 실제 출시 전 운영자가 직접 값을 넣고 실기기에서 확인해야 합니다.

- 공개 개인정보처리방침 URL: `LOVEDRAW_PRIVACY_POLICY_URL`
- 공개 계정/데이터 삭제 URL: `LOVEDRAW_ACCOUNT_DELETION_URL`
- 실제 문의 이메일: `LOVEDRAW_SUPPORT_EMAIL`
- 공개 운영자명: `LOVEDRAW_OPERATOR_NAME`
- 개인정보처리방침 시행일: `LOVEDRAW_POLICY_EFFECTIVE_DATE`
- 데이터 삭제 처리 기간: `LOVEDRAW_DELETION_PROCESSING_PERIOD`
- 운영 Firebase Realtime Database URL: `COUPLE_CANVAS_DATABASE_URL`
- 운영 `app/google-services.json`
- release signing keystore: `LOVEDRAW_RELEASE_STORE_FILE`, `LOVEDRAW_RELEASE_STORE_PASSWORD`, `LOVEDRAW_RELEASE_KEY_ALIAS`, `LOVEDRAW_RELEASE_KEY_PASSWORD`
- Firebase Console의 release SHA-1/SHA-256 등록
- Google Play Console의 Data safety, privacy policy, data deletion, foreground service special use 제출
- 실제 Android 기기 2대의 Google OAuth, 알림 권한, 다른 앱 위 표시 권한, 오버레이 동기화 증거

## 로컬 실기기 증거

증거 파일은 계정, 방 코드, 개인 화면, 알림 내용이 들어갈 수 있으므로 저장소에 커밋하지 않습니다. 기본 폴더는 `release-evidence/`이며, 다른 위치를 쓰려면 `LOVEDRAW_RELEASE_EVIDENCE_DIR`을 지정합니다.

필수 증거 접두어:

- `google-oauth`
- `permission-onboarding`
- `notification-permission`
- `overlay-permission`
- `overlay-notification-controls`
- `room-create-join`
- `invite-share`
- `two-device-overlay-sync`
- `clear-and-fadeout`
- `privacy-mode-widget`
- `privacy-legal-links`

검증:

```bash
./scripts/check_sensitive_files.sh
./scripts/verify_release_evidence.sh
./gradlew :app:verifyReleaseReadiness :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease
```

## 완료 판정

- 코드 완료: 위 고정 장치가 CI에서 통과하고 debug/release 빌드가 성공하면 코드 관점의 완료로 본다.
- 출시 완료: 외부 입력이 필요한 항목을 실제 값으로 채우고, `release-evidence/`에 실기기 증거를 남긴 뒤, Google Play Console 제출 항목까지 같은 내용으로 등록해야 완료로 본다.
