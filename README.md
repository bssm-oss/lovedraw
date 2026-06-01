# Couple Canvas

커플이 함께 그림을 그리고 기록을 남길 수 있는 Android 앱입니다.

Firebase를 이용해 방 생성, 코드 입장, 실시간 드로잉, 노트, 추억, 데이트 플랜, 퀴즈, Daily Spark, 위젯 데이터를 동기화합니다.

이 저장소에는 실제 Firebase 프로젝트 설정 파일을 넣지 않습니다. 각자 Firebase 프로젝트를 만든 뒤 로컬 설정 파일만 추가해서 실행합니다.

## 포함된 기능

- Kotlin + Jetpack Compose Android 앱
- Google 로그인 기반 Firebase Authentication
- Firebase Realtime Database 기반 방/드로잉 동기화
- Firebase Storage 기반 이미지/스냅샷 저장
- 화면 위에 그리는 오버레이 모드
- Jetpack Glance 위젯
- Firebase Database Rules, Storage Rules
- 단위 테스트와 Android instrumentation 테스트 소스

## Git에 올리지 않는 파일

아래 파일은 로컬에만 두고 커밋하지 않습니다.

- `.env`, `.env.*`
- `.firebaserc`
- `local.properties`
- `app/google-services.json`
- `app/google-services.json.*`
- `GoogleService-Info.plist`
- Firebase service account JSON
- Firebase 로그
- Firebase emulator export
- Gradle build 산출물
- 로컬 agent 상태 폴더

대신 아래 예시 파일만 커밋합니다.

- `.firebaserc.example`
- `.env.example`
- `app/google-services.example.json`

## 준비물

- Android Studio
- JDK 17
- Firebase CLI
- Firebase 프로젝트

Firebase 프로젝트에서는 아래 기능을 켜야 합니다.

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

   패키지 이름은 아래 값을 사용합니다.

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

6. `local.properties`에 Realtime Database URL을 추가합니다.

   Android Studio가 만든 `sdk.dir=...` 줄은 그대로 두고, 아래 값을 추가합니다.

   ```properties
   COUPLE_CANVAS_DATABASE_URL=https://your-project-id-default-rtdb.firebaseio.com
   ```

   Realtime Database가 regional URL을 쓰는 경우 Firebase Console에 표시된 URL을 그대로 넣습니다.

   ```properties
   COUPLE_CANVAS_DATABASE_URL=https://your-project-id-default-rtdb.asia-southeast1.firebasedatabase.app
   ```

7. Google 로그인을 켭니다.

   Firebase Console에서:

   - Authentication으로 이동
   - Sign-in method로 이동
   - Google provider 활성화
   - Google 로그인이 안 열리면 debug SHA-1 추가

   debug SHA-1은 아래 명령으로 확인합니다.

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

10. 테스트를 실행합니다.

    ```bash
    ./gradlew :app:testDebugUnitTest
    ```

## Firebase Emulator 사용하기

로컬 emulator를 쓰려면 `local.properties`에 아래 값을 추가합니다.

```properties
COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true
```

그 다음 emulator를 실행합니다.

```bash
firebase emulators:start --config firebase.debug.json --only auth,database,storage
```

다른 터미널에서 앱을 빌드하거나 테스트합니다.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

`scripts/verify_firebase_rules.sh`에서 사용할 수 있는 선택 환경변수는 `.env.example`에 정리되어 있습니다.

## 개발 시 주의사항

- 실제 Firebase 설정 파일은 커밋하지 않습니다.
- `app/google-services.json`은 로컬에만 둡니다.
- 실제 프로젝트를 가리키는 `.firebaserc`는 커밋하지 않습니다.
- `app/google-services.example.json`은 파일 구조 참고용입니다.
- debug 빌드는 기본적으로 실제 Firebase 프로젝트를 사용합니다.
- Firebase emulator를 쓸 때만 `COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true`를 켭니다.
