# lovedraw 개인정보와 권한 안내

이 문서는 출시 전 점검용 운영 문서입니다. 실제 스토어 제출 전에는 Google Play 정책, 개인정보 처리방침 URL, Firebase 설정, 앱 심사 화면의 Data safety 응답이 서로 같은 내용을 말하는지 다시 확인해야 합니다.

스토어 제출용 개인정보 처리방침 초안은 [docs/privacy-policy-ko.md](./docs/privacy-policy-ko.md)에 별도로 둔다.

Google Play 제출 전 외부 작업은 [docs/play-console-release-checklist.md](./docs/play-console-release-checklist.md)를 기준으로 확인한다.

Play Console Data safety 답변 초안은 [docs/play-console-data-safety-ko.md](./docs/play-console-data-safety-ko.md), foreground service special use 선언 초안은 [docs/foreground-service-special-use-ko.md](./docs/foreground-service-special-use-ko.md)에 둔다.

## 기본 원칙

- 로그인은 Google OAuth 기반 Firebase Auth로 처리한다.
- 앱 데이터는 Firebase Realtime Database와 Firebase Storage에 방 단위로 저장한다.
- 로컬 위젯 캐시와 임시 파일은 Android 자동 백업 대상에서 제외한다.
- 광고 SDK, 분석 SDK, 백그라운드 위치 추적, 몰래 위치 수집, Accessibility Service는 사용하지 않는다.
- 방 기록은 Google 계정으로 다시 로그인하면 복구할 수 있다.
- 권한 요청 직전 앱 안에서 왜 필요한지 짧게 설명한다.

## Firebase에 저장되는 데이터

- 계정 정보: Firebase uid, Google 표시 이름, 이메일, 프로필 사진 URL
- 방 정보: 방 이름, 초대 코드, 참여자 uid, 상태, 시작일, privacy mode
- 실시간 그림: stroke, activeStroke, drawing snapshot
- 러브노트, 데이트 플랜, 버킷리스트, 추억, 퀴즈, Daily Spark, 통계
- 사용자가 직접 선택한 추억/답변 이미지와 낙서 snapshot 이미지

## 권한

- Internet: Firebase 로그인, 데이터 동기화, 이미지 업로드에 필요하다.
- Network state: Firebase 연결 상태 처리에 사용한다.
- 알림: 화면 위 그리기를 foreground service로 실행하고, 알림에서 그리기 시작/끄기/전체 지우기를 제공하는 데 사용한다.
- 다른 앱 위 표시: 사용자가 직접 허용했을 때만 홈 화면과 다른 앱 위에 낙서를 보여준다.
- Location: Distance Widget에서 사용자가 동의하고 버튼을 눌렀을 때만 1회 위치 공유에 사용한다.
- 전체 갤러리 읽기 권한은 요청하지 않는다. 이미지는 Android Photo Picker로 사용자가 선택한 것만 처리한다.

## 다른 앱 위 표시 정책

- 기본값은 OFF다.
- Android 설정의 "다른 앱 위에 표시" 권한을 사용자가 직접 허용해야 켤 수 있다.
- 켜져 있는 동안에는 foreground service 알림과 화면 위 미니 카드가 함께 표시된다.
- 앱을 홈으로 내리거나 다른 앱을 열어도 사용자가 선택한 방의 낙서 오버레이가 계속 보인다.
- 카드의 닫기 버튼, 알림의 숨기기 액션, 앱 홈의 끄기 버튼으로 언제든 중지할 수 있다.
- Privacy Mode가 켜져 있으면 미니 카드도 노트 본문, 데이트 제목, 질문 내용을 직접 노출하지 않는다.
- 몰래 켜거나, 사용자가 권한을 허용하지 않았는데 표시하거나, 백그라운드에서 자동으로 다시 켜는 동작은 하지 않는다.
- 화면을 캡처하거나, 다른 앱의 텍스트/이미지 내용을 읽거나, 키 입력을 수집하지 않는다.
- 오버레이는 사용자가 그린 선과 도구 UI만 표시한다.

## 초대 공유

- 초대 코드는 앱 안 QR, 링크 복사, Android 공유 메뉴, 카카오톡/인스타그램 공유 인텐트로 보낼 수 있다.
- 카카오/인스타 SDK를 앱에 포함하지 않는다. 해당 앱이 설치되어 있을 때 Android 공유 인텐트로 텍스트만 전달한다.
- 초대 링크 형식은 `lovedraw://invite?code=XXXXXX`이다.
- 초대 코드를 아는 사람은 입장을 시도할 수 있으므로, 코드는 사용자가 직접 보내는 대상에게만 공유하도록 안내한다.

## 위치 공유 정책

- 기본값은 OFF다.
- 한쪽만 켜서는 거리 표시가 완료되지 않는다.
- 위치 공유 중임을 UI에 표시한다.
- 백그라운드에서 계속 위치를 가져오지 않는다.
- 사용자가 버튼을 눌렀을 때 현재 위치를 1회 공유한다.
- 연결이 끊겼거나 새 위치가 없으면 마지막 공유 기준으로 표시한다.

## 위젯 Privacy Mode

- Privacy Mode가 켜져 있으면 위젯에서 노트 본문과 사진 내용을 숨긴다.
- 위젯에는 "앱에서 확인하기", "새 노트가 있어요"처럼 민감도가 낮은 문구만 표시한다.
- DrawingPreviewWidget과 MemoryWidget은 privacy mode에서 이미지를 직접 노출하지 않는다.
- 위젯은 홈 화면과 지원 기기의 잠금화면 카테고리에 등록되며, 잠금화면에서도 Privacy Mode 문구를 우선 적용한다.

## Google Play Data safety 초안

스토어 제출 전 Play Console의 Data safety는 실제 Firebase/권한/스토리지 사용과 일치해야 한다.

- Account info: Google 로그인 표시 이름, 이메일, 프로필 사진 URL을 Firebase Auth와 앱 계정 식별에 사용한다.
- User-generated content: 낙서, 러브노트, 데이트 플랜, 버킷, 퀴즈 답변, Daily Spark, 추억 메모를 방 동기화에 사용한다.
- Photos and videos: 사용자가 Photo Picker로 직접 고른 이미지만 추억/퀴즈/스냅샷 기능에 사용한다.
- Approximate/Precise location: Distance Widget을 사용자가 켠 경우에만 1회 위치 공유에 사용한다.
- App activity/Analytics: 별도 분석 SDK는 사용하지 않는다.
- Sharing: Firebase와 Google 로그인 제공자, 사용자가 직접 선택한 공유 대상 앱으로만 필요한 데이터가 전달된다.
- Security practices: Firebase 전송 구간 보안, 인증 기반 Rules, 민감 로그 금지를 기준으로 작성한다.

## Play Console 제출 메모

- 민감 권한은 현재 앱 기능에 필요한 경우에만 요청해야 한다.
- 알림과 다른 앱 위 표시는 사용자가 낙서 오버레이 기능을 이해한 뒤 요청한다.
- Android 14 이상 foreground service 사용을 위해 Play Console App content에서 special use foreground service 사용 사유를 선언한다.
- Data safety, 개인정보 처리방침, 앱 내부 권한 안내가 서로 다른 말을 하지 않도록 한다.

## 출시 전 점검

- Firebase Auth Google provider 활성화
- 최신 `google-services.json` 반영
- `app/google-services.json`의 Android client에 `oauth_client`가 1개 이상 포함되는지 확인한다:
  `jq '.client[0].oauth_client | length' app/google-services.json`
- 현재 Firebase Android 앱에는 debug SHA-1이 등록되어 있어야 한다. 확인:
  `firebase apps:android:sha:list 1:814622180622:android:e5350cb95627fa9f24035f --project lovedraw-56139`
- Google provider를 켠 뒤 Android 설정을 다시 내려받는다:
  `firebase apps:sdkconfig android 1:814622180622:android:e5350cb95627fa9f24035f --project lovedraw-56139 > app/google-services.json`
- Firebase Realtime Database rules 배포
- Firebase Storage rules 배포
- Production Realtime Database rules는 Auth Emulator ID token 기반 스모크 테스트로 검증한다:
  `firebase emulators:exec --config firebase.json --project lovedraw-56139 --only auth,database 'bash scripts/verify_firebase_rules.sh'`
- 실제 기기 2대로 방 생성, 코드 입장, 실시간 드로잉, 이미지 업로드, 위젯 privacy mode 테스트
- 다른 앱 위 표시 권한 허용/거부, 홈 화면 위 표시, 다른 앱 위 표시, 닫기/알림 숨기기 테스트
- 위치 권한 거부/허용, 한쪽만 동의, 양쪽 동의, 마지막 공유 기준 표시 테스트
- 카카오톡/인스타그램 미설치 상태와 설치 상태에서 초대 공유 fallback 테스트
- QR 스캔으로 `lovedraw://invite?code=XXXXXX` 링크가 앱을 열고 코드 입장을 시도하는지 테스트
- 릴리스 빌드 전 `git ls-files`로 `google-services.json`, `firebase-debug.log`, `database-debug.log`, `.env`가 추적되지 않는지 확인

## 참고 정책

- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play permissions and APIs that access sensitive information: https://support.google.com/googleplay/android-developer/answer/9888170
- Google Play Data safety section: https://support.google.com/googleplay/android-developer/answer/10787469
- Android foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
