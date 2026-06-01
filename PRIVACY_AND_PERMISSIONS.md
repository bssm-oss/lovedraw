# Couple Canvas 개인정보와 권한 안내

## 기본 원칙

- 로그인은 Google OAuth 기반 Firebase Auth로 처리한다.
- 앱 데이터는 Firebase Realtime Database와 Firebase Storage에 방 단위로 저장한다.
- 로컬 위젯 캐시와 임시 파일은 Android 자동 백업 대상에서 제외한다.
- 광고 SDK, 분석 SDK, 백그라운드 위치 추적, 몰래 위치 수집은 사용하지 않는다.
- 방 기록은 Google 계정으로 다시 로그인하면 복구할 수 있다.

## Firebase에 저장되는 데이터

- 방 정보: 방 이름, 초대 코드, 참여자 uid, 상태, 시작일, privacy mode
- 실시간 그림: stroke, activeStroke, drawing snapshot
- 러브노트, 데이트 플랜, 버킷리스트, 추억, 퀴즈, Daily Spark, 통계
- 사용자가 직접 선택한 추억/답변 이미지와 낙서 snapshot 이미지

## 권한

- Internet: Firebase 로그인, 데이터 동기화, 이미지 업로드에 필요하다.
- Network state: Firebase 연결 상태 처리에 사용한다.
- 다른 앱 위 표시: 사용자가 홈 화면에서 직접 켤 때만 Couple Canvas 미니 카드를 다른 앱 위에 보여준다.
- Location: Distance Widget에서 사용자가 동의하고 버튼을 누를 때만 1회 위치 공유에 사용한다.
- 알림 권한은 요청하지 않는다.
- 전체 갤러리 읽기 권한은 요청하지 않는다. 이미지는 Android Photo Picker로 사용자가 선택한 것만 처리한다.

## 다른 앱 위 표시 정책

- 기본값은 OFF다.
- Android 설정의 "다른 앱 위에 표시" 권한을 사용자가 직접 허용해야 켤 수 있다.
- 켜져 있는 동안에는 foreground service 알림과 화면 위 미니 카드가 함께 표시된다.
- 앱을 홈으로 내리거나 다른 앱을 열어도 미니 카드는 계속 보인다.
- 카드의 닫기 버튼, 알림의 숨기기 액션, 앱 홈의 끄기 버튼으로 언제든 중지할 수 있다.
- Privacy Mode가 켜져 있으면 미니 카드도 노트 본문, 데이트 제목, 질문 내용을 직접 노출하지 않는다.
- 몰래 켜거나, 사용자가 권한을 허용하지 않았는데 표시하거나, 백그라운드에서 자동으로 다시 켜는 동작은 하지 않는다.

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
