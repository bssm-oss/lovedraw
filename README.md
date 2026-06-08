# lovedraw

커플이 같은 방에 들어와 화면 위에 바로 낙서하고, 노트/추억/데이트 플랜/퀴즈/Daily Spark를 함께 기록하는 Android 앱입니다.

앱 코드는 `com.example.couplecanvas` 패키지를 사용하고, 현재 앱 이름은 `Couple Canvas`로 유지되어 있습니다. 화면 브랜드는 `lovedraw` 톤에 맞춰 정리 중입니다.

## 핵심 기능

- Google OAuth 기반 Firebase 로그인
- 방 만들기 / 코드 입장 / 여러 방 관리
- Firebase Realtime Database 기반 실시간 드로잉 동기화
- 화면 위에 직접 그리는 Android 오버레이 모드
- 브러시 색상, 두께, 지우개, undo, clear
- 일정 시간 뒤 사라지는 하이라이트/레이저 포인터형 스트로크
- 카카오톡/인스타그램 공유, QR 코드, 초대 링크
- Firebase Storage 기반 이미지, 드로잉 스냅샷 저장
- 러브노트, 추억, 데이트 플래너, 버킷리스트, 퀴즈, Daily Spark
- Jetpack Glance 기반 홈 화면 위젯
- privacy mode 기반 위젯 민감 정보 숨김

## 기술 스택

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Coroutines + Flow
- Navigation Compose
- Firebase Authentication
- Firebase Realtime Database
- Firebase Storage
- Jetpack Glance App Widget

## 저장소에 올리지 않는 파일

실제 Firebase 프로젝트 정보, 로컬 SDK 경로, API 키, 로그 파일은 커밋하지 않습니다.

커밋 금지:

- `.env`, `.env.*`
- `.firebaserc`
- `local.properties`
- `app/google-services.json`
- `app/google-services.json.*`
- `GoogleService-Info.plist`
- Firebase service account JSON
- Firebase emulator export
- Firebase/Gradle/Android 로그
- 빌드 산출물
- 로컬 agent/runtime 상태 폴더

대신 아래 예시 파일만 커밋합니다.

- `.env.example`
- `.firebaserc.example`
- `app/google-services.example.json`
- `keystore.properties.example`

## 준비물

- Android Studio
- JDK 17
- Android SDK
- Firebase CLI
- Firebase 프로젝트
- Google 계정 로그인 테스트가 가능한 Android 기기 또는 에뮬레이터

Firebase Console에서 켜야 하는 기능:

- Authentication
- Google 로그인 provider
- Realtime Database
- Storage

## 처음 설정하기

1. 저장소를 클론합니다.

   ```bash
   git clone https://github.com/heodongun/lovedraw.git
   cd lovedraw
   ```

2. Firebase Console에서 새 프로젝트를 만듭니다.

3. Firebase 프로젝트에 Android 앱을 추가합니다.

   패키지 이름:

   ```text
   com.example.couplecanvas
   ```

4. Firebase Console에서 `google-services.json`을 다운로드합니다.

   아래 위치에 넣습니다.

   ```text
   app/google-services.json
   ```

5. `.firebaserc`를 만듭니다.

   ```bash
   cp .firebaserc.example .firebaserc
   ```

   `.firebaserc` 안의 `your-firebase-project-id`를 본인 Firebase 프로젝트 ID로 바꿉니다.

6. `local.properties`를 확인합니다.

   Android Studio가 만든 `sdk.dir=...` 줄은 그대로 둡니다.

   그 아래에 Realtime Database URL을 추가합니다.

   ```properties
   COUPLE_CANVAS_DATABASE_URL=https://your-project-id-default-rtdb.firebaseio.com
   ```

   Realtime Database가 regional URL을 쓰는 경우 Firebase Console에 표시된 URL을 그대로 넣습니다.

   ```properties
   COUPLE_CANVAS_DATABASE_URL=https://your-project-id-default-rtdb.asia-southeast1.firebasedatabase.app
   ```

7. 출시용 운영 링크를 준비합니다.

   실제 출시 전에는 앱 설정 화면과 Play Console에 들어갈 공개 URL/연락처를 넣어야 합니다. 개발 중에는 비워둘 수 있지만, 출시 전에는 반드시 실제 값으로 채웁니다.

   ```properties
   LOVEDRAW_PRIVACY_POLICY_URL=https://your-domain.example/privacy
   LOVEDRAW_ACCOUNT_DELETION_URL=https://your-domain.example/delete-account
   LOVEDRAW_SUPPORT_EMAIL=support@your-domain.example
   ```

8. Google 로그인을 설정합니다.

   Firebase Console에서:

   - Authentication으로 이동
   - Sign-in method로 이동
   - Google provider 활성화
   - Android 앱의 debug SHA-1 추가
   - `google-services.json`을 다시 다운로드해서 `app/google-services.json`에 덮어쓰기

   debug SHA-1 확인:

   ```bash
   ./gradlew signingReport
   ```

8. Firebase rules를 배포합니다.

   ```bash
   firebase login
   firebase use your-firebase-project-id
   firebase deploy --only database,storage
   ```

9. 앱을 빌드합니다.

   ```bash
   ./gradlew :app:assembleDebug
   ```

10. 단위 테스트를 실행합니다.

    ```bash
    ./gradlew :app:testDebugUnitTest
    ```

11. 출시 직전 설정을 검증합니다.

    실제 Play Store 업로드 전에 아래 태스크가 통과해야 합니다.

    ```bash
    ./gradlew :app:verifyReleaseReadiness
    ```

    이 태스크는 운영 Firebase URL, 공개 개인정보처리방침 URL, 계정/데이터 삭제 URL, 문의 이메일, release signing, 로컬 `app/google-services.json` 존재 여부를 확인합니다. Play Console 입력값과 Firebase Console의 release SHA 등록은 콘솔에서 별도로 확인해야 합니다.

## Firebase Emulator로 계정 없이 테스트하기

실제 Google 계정 선택 플로우 없이 빠르게 테스트하려면 Firebase Emulator 모드를 사용합니다.

`local.properties`에 아래 값을 추가합니다.

```properties
COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true
```

Firebase Emulator를 실행합니다.

```bash
firebase emulators:start --config firebase.debug.json --only auth,database,storage
```

다른 터미널에서 앱을 빌드합니다.

```bash
./gradlew :app:assembleDebug
```

에뮬레이터에 설치합니다.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

두 대의 에뮬레이터에서 테스트할 때는 각각 같은 APK를 설치한 뒤, 한쪽에서 방을 만들고 다른 쪽에서 코드로 입장합니다.

## 자주 쓰는 명령

빌드:

```bash
./gradlew :app:assembleDebug
```

단위 테스트:

```bash
./gradlew :app:testDebugUnitTest
```

Android instrumentation 테스트:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Firebase rules 배포:

```bash
firebase deploy --only database,storage
```

Firebase rules 검증 스크립트:

```bash
./scripts/verify_firebase_rules.sh
```

## Firebase 설정 파일 예시

`.firebaserc.example`

```json
{
  "projects": {
    "default": "your-firebase-project-id"
  }
}
```

`app/google-services.example.json`은 실제 파일 구조 참고용입니다. 실제 앱 실행에는 Firebase Console에서 받은 `app/google-services.json`이 필요합니다.

## 개인정보와 권한

앱은 커플 간 기록을 다루기 때문에 권한과 개인정보 노출을 보수적으로 처리합니다.

- Google 로그인은 계정 식별과 기록 복구를 위해 사용합니다.
- 알림 권한은 오버레이 그리기 시작/끄기/전체 지우기 액션에 사용합니다.
- 화면 위 그리기 기능은 사용자가 직접 켠 경우에만 동작합니다.
- lovedraw는 화면 내용을 캡처하거나 다른 앱 내용을 읽지 않습니다.
- 위젯은 privacy mode가 켜져 있으면 민감한 문구와 이미지를 숨깁니다.
- 위치 공유 기능은 기본값 OFF이며, 양쪽 동의가 있을 때만 거리 표시용으로 사용합니다.
- 민감한 데이터는 로그에 남기지 않는 것을 원칙으로 합니다.

권한/개인정보 정책 상세 문서는 [PRIVACY_AND_PERMISSIONS.md](./PRIVACY_AND_PERMISSIONS.md)를 확인하세요.

스토어 제출용 개인정보 처리방침 초안은 [docs/privacy-policy-ko.md](./docs/privacy-policy-ko.md)에 있습니다.

Google Play 제출 체크리스트는 [docs/play-console-release-checklist.md](./docs/play-console-release-checklist.md)에 있습니다.

Data Safety 답변 초안은 [docs/play-console-data-safety-ko.md](./docs/play-console-data-safety-ko.md), Foreground Service special use 선언 초안은 [docs/foreground-service-special-use-ko.md](./docs/foreground-service-special-use-ko.md)에 있습니다.

릴리스 서명은 `keystore.properties.example`을 `keystore.properties`로 복사한 뒤 로컬 비밀값을 채우거나, 같은 이름의 환경변수로 설정합니다. `keystore.properties`와 실제 keystore 파일은 커밋하지 않습니다.

새 upload keystore가 필요하면 아래 스크립트를 사용할 수 있습니다.

```bash
./scripts/create_release_keystore.sh
./gradlew :app:bundleRelease
```

## 테스트 체크리스트

릴리즈 전 최소 확인 항목:

- Google 로그인 성공
- 로그아웃 후 재로그인 시 기존 기록 확인
- 방 만들기 / 코드 입장
- 이미 꽉 찬 방 입장 차단
- 없는 코드 입력 처리
- 두 기기 실시간 드로잉 동기화
- 브러시, 지우개, undo, clear
- 하이라이트 스트로크 자동 사라짐
- 오버레이 권한 안내와 실제 오버레이 드로잉
- 카카오톡/인스타그램 공유 intent
- QR 코드와 `lovedraw://invite?code=XXXXXX` 초대 링크
- 초대 링크 클릭 후 자동 코드 입장
- 위젯 privacy mode
- 네트워크 끊김/재연결
- Firebase Database Rules
- Firebase Storage Rules

## 문제 해결

Google 로그인 버튼이 동작하지 않을 때:

- Firebase Console에서 Google provider가 켜져 있는지 확인합니다.
- debug SHA-1을 Firebase Android 앱에 추가했는지 확인합니다.
- `google-services.json`을 최신 파일로 다시 받았는지 확인합니다.

Realtime Database에 연결되지 않을 때:

- `local.properties`의 `COUPLE_CANVAS_DATABASE_URL`이 실제 DB URL과 같은지 확인합니다.
- regional DB URL이면 `asia-southeast1.firebasedatabase.app` 형태를 그대로 사용합니다.
- Emulator 모드라면 Firebase Emulator가 실행 중인지 확인합니다.

에뮬레이터에서 Firebase Emulator에 연결되지 않을 때:

- 앱은 Android 에뮬레이터에서 호스트 머신을 `10.0.2.2`로 접근합니다.
- `COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true`가 debug 빌드에 들어갔는지 확인합니다.
- `firebase.debug.json`의 포트가 앱 설정과 같은지 확인합니다.

빌드가 실패할 때:

- JDK 17을 사용 중인지 확인합니다.
- Android SDK 경로가 `local.properties`의 `sdk.dir`에 들어있는지 확인합니다.
- `app/google-services.json`이 있는지 확인합니다.

## 개발 원칙

- 실제 Firebase 설정과 API 키는 저장소에 올리지 않습니다.
- 실행 가능한 예시는 example 파일로만 제공합니다.
- 사용자에게 보이는 로그인은 Google OAuth만 사용합니다.
- 테스트용 익명/디버그 로그인은 Firebase Emulator 모드에서만 사용합니다.
- 민감한 내용은 로그, 위젯, 공유 카드에 노출되지 않게 처리합니다.
