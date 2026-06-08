# lovedraw Google Play 출시 체크리스트

이 체크리스트는 코드 저장소 밖에서 처리해야 하는 Google Play 제출 항목을 정리한 문서입니다. 체크리스트를 완료하기 전까지는 `app-release-unsigned.apk`만으로 실제 출시할 수 없습니다.

참고해야 할 Google 공식 문서:

- [Data safety 섹션 작성 안내](https://support.google.com/googleplay/android-developer/answer/10787469)
- [앱 계정 삭제 요구사항](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Google Play 개발자 프로그램 정책](https://support.google.com/googleplay/android-developer/answer/15402170)
- [Foreground service 요구사항](https://support.google.com/googleplay/android-developer/answer/13392821)

출시 직전에는 로컬 설정이 실제 값으로 채워졌는지 먼저 확인합니다.

```bash
./gradlew :app:verifyReleaseReadiness
```

이 태스크는 Firebase 운영 URL, 개인정보처리방침 URL, 계정/데이터 삭제 URL, 문의 이메일, release signing, `app/google-services.json` 존재 여부를 점검합니다. Play Console 입력값, Firebase Console의 release SHA 등록 여부, 실제 웹 페이지 공개 상태는 로컬에서 완전히 검증할 수 없으므로 별도로 확인해야 합니다.

## 1. 릴리스 서명

- Google Play App Signing 사용 여부를 결정한다.
- release keystore를 생성하거나 기존 키를 준비한다.
- `keystore.properties` 또는 CI secret에만 저장하고 저장소에는 커밋하지 않는다.
- `keystore.properties.example`을 참고해 로컬 `keystore.properties`를 만든다.
- 새 upload keystore를 만들 때는 아래 스크립트를 사용할 수 있다.

```bash
./scripts/create_release_keystore.sh
```

이 스크립트는 `release/lovedraw-upload.keystore`와 `keystore.properties`를 만든다. 둘 다 저장소에 커밋하면 안 된다.

- 같은 값은 CI에서 환경변수로도 설정할 수 있다.
- 값 이름:
  - `LOVEDRAW_RELEASE_STORE_FILE`
  - `LOVEDRAW_RELEASE_STORE_PASSWORD`
  - `LOVEDRAW_RELEASE_KEY_ALIAS`
  - `LOVEDRAW_RELEASE_KEY_PASSWORD`
- 최종 산출물은 APK보다 AAB 권장:

```bash
./gradlew :app:verifyReleaseReadiness :app:bundleRelease
```

release signing 값이 없으면 debug/release 검증 빌드는 가능하지만 Play Console에 올릴 최종 서명 산출물은 만들 수 없다.

## 2. Firebase 운영 설정

- Firebase Auth Google provider 활성화
- release SHA-1/SHA-256을 Firebase Android 앱에 등록
- 최신 `app/google-services.json`을 로컬에만 반영
- Realtime Database rules 배포
- Storage rules 배포
- debug/emulator 설정이 release 빌드에 들어가지 않는지 확인

## 3. 개인정보 처리방침 URL

- [docs/privacy-policy-ko.md](./privacy-policy-ko.md)를 실제 운영자 정보로 수정한다.
- [docs/account-data-deletion-page-ko.md](./account-data-deletion-page-ko.md)를 실제 운영자 정보로 수정한다.
- 공개 URL에 게시한다.
- Play Console App content > Privacy policy에 같은 URL을 등록한다.
- Play Console Data safety > Data deletion에 계정/데이터 삭제 URL을 등록한다.
- 앱 내부 권한 안내, README, Data safety 답변과 내용이 서로 맞는지 확인한다.

필수로 채울 항목:

- 운영자명
- 문의 이메일
- 개인정보 처리방침 시행일
- 데이터 삭제 요청 방법

앱 설정 화면에서 열릴 운영 링크도 `local.properties`, Gradle property, CI 환경변수 중 하나로 설정한다.

```properties
LOVEDRAW_PRIVACY_POLICY_URL=https://your-domain.example/privacy
LOVEDRAW_ACCOUNT_DELETION_URL=https://your-domain.example/delete-account
LOVEDRAW_SUPPORT_EMAIL=support@your-domain.example
LOVEDRAW_OPERATOR_NAME=your-public-operator-name
LOVEDRAW_POLICY_EFFECTIVE_DATE=2026-06-08
```

이 값이 비어 있으면 앱 설정 화면에서 해당 버튼이 비활성화되고, `:app:verifyReleaseReadiness`가 실패한다.

## 4. Play Console Data safety

앱의 실제 동작 기준으로 작성한다.

- 상세 답변 초안은 [docs/play-console-data-safety-ko.md](./play-console-data-safety-ko.md)를 기준으로 작성한다.
- Account info: Firebase uid, Google 표시 이름, 이메일, 프로필 사진 URL
- User-generated content: 낙서, 러브노트, 데이트 플랜, 버킷, 퀴즈 답변, Daily Spark, 추억 메모
- Photos and videos: 사용자가 Photo Picker로 직접 선택한 이미지
- Location: Distance Widget에서 양쪽 동의 후 버튼을 누른 경우의 1회 위치 공유
- App activity/Analytics: 별도 분석 SDK 없음
- Sharing: Firebase/Google 로그인 제공자와 사용자가 직접 선택한 공유 앱

## 5. Foreground Service 및 권한 선언

Play Console App content에서 foreground service special use를 설명한다.

상세 선언 초안은 [docs/foreground-service-special-use-ko.md](./foreground-service-special-use-ko.md)를 기준으로 작성한다.

권장 설명:

```text
lovedraw uses a user-enabled foreground service to show a drawing overlay over other apps. The overlay lets the user draw directly on the screen and sync strokes with the selected room. The service is visible through a persistent notification and can be stopped by the user at any time.
```

권한 설명이 앱 내부 문구와 일치하는지 확인한다.

- 알림: 그리기 시작/끄기/전체 지우기 조작
- 다른 앱 위 표시: 화면 위 낙서 오버레이
- 위치: Distance Widget에서 양쪽 동의 후 1회 공유

심사용 데모와 앱 첫 실행 화면에서는 권한 요청 전에 아래 내용이 사용자에게 먼저 보여야 한다.

- `처음 한 번만 권한을 설정해 주세요.`
- `다른 앱 위에 표시 권한은 지금 보는 화면 위에 선을 띄우는 데 사용해요.`
- `lovedraw는 화면 내용을 읽거나 저장하지 않아요.`

## 6. 실제 기기 QA

최소 두 대의 실제 Android 기기에서 확인한다.

- 첫 실행 권한 안내
- 권한 요청 전에 알림/다른 앱 위 표시가 왜 필요한지 표시
- Google 로그인
- 방 만들기
- 초대 코드 입장
- QR 스캔으로 `lovedraw://invite?code=XXXXXX` 열기
- 카카오톡 설치/미설치 공유 fallback
- 인스타그램 설치/미설치 공유 fallback
- Android 11 이상 기기에서 카카오톡/인스타그램 직접 공유 버튼이 설치 앱을 감지하는지 확인
- 홈 화면 위 오버레이 그리기
- 다른 앱 위 오버레이 그리기
- 상대 화면 실시간 동기화
- 전체 지우기
- 일정 시간 후 fade-out
- 네트워크 끊김 후 `재연결 중` 표시
- 재연결 후 `연결됨` 표시
- Privacy Mode 위젯 민감 정보 숨김
- 위치 공유 OFF 기본값
- 한쪽만 위치 공유 동의한 상태
- 양쪽 동의 후 1회 위치 공유

## 7. 저장소 업로드 전 확인

```bash
git ls-files firebase-debug.log database-debug.log '*.log' '.env' '.env.*' 'app/google-services.json' 'app/src/*/google-services.json' '.firebaserc'
```

출력은 `.env.example` 같은 example 파일만 허용한다.

```bash
./gradlew :app:verifyReleaseReadiness
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease
```

Google Play에 올리기 전에는 release signing 또는 AAB 빌드도 별도로 확인한다.
