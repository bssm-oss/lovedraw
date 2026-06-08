# lovedraw 개인정보 처리방침 초안

시행일: `LOVEDRAW_POLICY_EFFECTIVE_DATE` 값으로 확정

이 문서는 lovedraw Android 앱의 개인정보 처리 기준을 설명합니다. 실제 배포 전에는 앱 운영자명(`LOVEDRAW_OPERATOR_NAME`), 연락처(`LOVEDRAW_SUPPORT_EMAIL`), 개인정보 처리방침 URL(`LOVEDRAW_PRIVACY_POLICY_URL`), Google Play Console Data safety 답변과 내용이 일치하도록 최종 검토해야 합니다.

## 1. 수집하는 정보

lovedraw는 Google 로그인과 커플 방 동기화를 위해 다음 정보를 처리할 수 있습니다.

- 계정 정보: Firebase 사용자 ID, Google 표시 이름, 이메일, 프로필 사진 URL
- 방 정보: 방 이름, 초대 코드, 참여자 ID, 방 상태, 시작일, Privacy Mode 설정
- 사용자가 만든 콘텐츠: 화면 위 낙서, 러브노트, 데이트 플랜, 버킷리스트, 추억, 퀴즈 답변, Daily Spark 답변
- 이미지: 사용자가 Android Photo Picker로 직접 선택한 이미지와 저장한 낙서 스냅샷
- 위치 정보: Distance Widget에서 양쪽이 동의하고 사용자가 버튼을 누른 경우의 현재 위치 1회 공유값
- 기기/연결 정보: Firebase 연결 상태 처리에 필요한 네트워크 상태

## 2. 정보를 사용하는 목적

수집한 정보는 다음 목적에만 사용합니다.

- Google 계정 로그인과 기록 복구
- 방 만들기, 초대 코드 입장, 여러 방 관리
- Firebase Realtime Database 기반 실시간 낙서 동기화
- 러브노트, 데이트 플랜, 추억, 퀴즈, Daily Spark, 위젯 표시
- Privacy Mode에 따른 위젯 민감 정보 숨김
- Distance Widget에서 양쪽 동의 후 거리 표시
- 앱 오류 방지, 연결 상태 표시, 보안 규칙 적용

광고 SDK, 별도 분석 SDK, 몰래 위치 추적, 백그라운드 지속 위치 수집, Accessibility Service를 사용하지 않습니다.

## 3. 권한 사용

### 알림

알림 권한은 화면 위 그리기 기능을 foreground service로 실행하고, 알림에서 그리기 시작, 그리기 끄기, 전체 지우기 액션을 제공하기 위해 사용합니다.

### 다른 앱 위에 표시

이 권한은 사용자가 홈 화면이나 다른 앱 위에서 낙서를 보고 그릴 수 있도록 오버레이를 표시하기 위해 사용합니다. lovedraw는 화면 내용을 캡처하거나, 다른 앱의 텍스트/이미지를 읽거나, 키 입력을 수집하지 않습니다.

### 위치

위치 권한은 Distance Widget을 사용자가 직접 켠 경우에만 사용합니다. 기본값은 꺼짐이며, 양쪽이 동의하고 사용자가 버튼을 눌렀을 때 현재 위치를 1회 공유합니다. 백그라운드에서 계속 위치를 가져오지 않습니다.

### 사진/이미지

전체 갤러리 읽기 권한을 요청하지 않습니다. 이미지는 Android Photo Picker로 사용자가 직접 선택한 항목만 처리합니다.

## 4. 정보 저장과 동기화

- 계정과 방 데이터는 Firebase Authentication, Firebase Realtime Database, Firebase Storage에 저장됩니다.
- 초대 링크는 `lovedraw://invite?code=XXXXXX` 형식이며, 사용자가 직접 공유한 상대가 방 입장을 시도할 수 있습니다.
- 로컬 위젯 캐시와 임시 파일은 앱 기능 표시를 위해 기기 안에 저장될 수 있습니다.
- 민감한 내용은 로그에 남기지 않는 것을 원칙으로 합니다.

## 5. 제3자 제공과 처리 위탁

lovedraw는 앱 기능 제공을 위해 다음 서비스를 사용합니다.

- Google Firebase Authentication: Google 계정 로그인
- Firebase Realtime Database: 방 데이터와 실시간 낙서 동기화
- Firebase Storage: 사용자가 선택한 이미지와 낙서 스냅샷 저장
- 사용자가 직접 선택한 공유 앱: 카카오톡, 인스타그램, Android 공유 메뉴 등으로 초대 텍스트 전달

사용자가 직접 공유 버튼을 누른 경우를 제외하고, 카카오톡/인스타그램 SDK를 통해 별도 데이터를 자동 전송하지 않습니다.

## 6. 보관 기간과 삭제

사용자가 방을 나가거나 콘텐츠를 삭제하면 앱은 해당 요청을 Firebase 데이터에 반영합니다. Google 계정으로 다시 로그인하면 남아 있는 방 기록을 복구할 수 있습니다.

계정 또는 데이터 삭제 요청 절차는 공개 삭제 요청 URL(`LOVEDRAW_ACCOUNT_DELETION_URL`)과 운영자 연락처(`LOVEDRAW_SUPPORT_EMAIL`)에 맞춰 확정해야 합니다.

## 7. 이용자의 선택권

사용자는 다음을 직접 선택할 수 있습니다.

- Google 계정 로그인/로그아웃
- 방 생성, 입장, 보관, 나가기
- 오버레이 그리기 시작/끄기
- Privacy Mode 켜기/끄기
- Distance Widget 위치 공유 동의 켜기/끄기
- 위치 1회 공유 실행 여부
- Photo Picker를 통한 이미지 선택 여부

Android 설정에서 알림, 다른 앱 위에 표시, 위치 권한을 언제든 변경할 수 있습니다.

## 8. 아동 및 민감 정보

lovedraw는 커플 간 기록 앱이며, 민감하거나 노골적인 콘텐츠 생성을 목적으로 하지 않습니다. 데이트 추천과 퀴즈 질문은 안전하고 건전한 내용으로 구성합니다.

## 9. 보안

- Firebase 인증 기반 보안 규칙을 사용합니다.
- Firebase 전송 구간 보안과 플랫폼 보안 기능을 사용합니다.
- 앱은 화면 내용을 몰래 캡처하거나 키 입력을 수집하지 않습니다.
- 릴리스 전 `google-services.json`, `.env`, Firebase debug log, service account 파일이 저장소에 포함되지 않았는지 확인합니다.

## 10. 문의

운영자: `LOVEDRAW_OPERATOR_NAME`

이메일: `LOVEDRAW_SUPPORT_EMAIL`

개인정보 처리방침 URL: `LOVEDRAW_PRIVACY_POLICY_URL`
