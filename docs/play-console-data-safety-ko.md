# lovedraw Play Console Data Safety 답변 초안

이 문서는 Play Console > App content > Data safety 섹션을 작성할 때 참고하는 초안이다. 최종 제출 전에는 실제 배포 빌드, Firebase 설정, 개인정보 처리방침 공개 URL, 앱 내부 권한 안내가 모두 같은 내용을 말하는지 다시 대조해야 한다.

공식 정책 기준으로 개발자는 Data safety 답변을 명확하고 정확하게 유지해야 하며, 앱 동작과 개인정보 처리방침 사이에 불일치가 있으면 심사 문제가 될 수 있다.

## 기본 답변 방향

- 앱이 사용자 데이터를 수집하나요: 예
- 데이터가 암호화되어 전송되나요: 예
- 사용자가 데이터 삭제를 요청할 수 있나요: 예로 제출하려면 실제 삭제 요청 URL 또는 이메일 절차를 먼저 공개해야 함
- 광고 목적 데이터 사용: 아니오
- 분석 SDK 사용: 아니오
- 사용자의 데이터를 판매하나요: 아니오
- 앱 카테고리를 아동 대상 또는 Families로 제출할 예정인가: 아니오로 운영할 것을 권장

## 수집 데이터 분류

### Personal info

수집 또는 처리:

- 이름: Google 계정 표시 이름
- 이메일 주소: Google 로그인 이메일
- 사용자 ID: Firebase uid
- 사진: Google 프로필 사진 URL

용도:

- App functionality
- Account management

공유:

- Firebase Authentication/Google 로그인 제공자와 앱 기능 제공 목적의 처리
- 사용자가 직접 공유 버튼을 누른 경우 초대 텍스트가 선택한 공유 앱으로 전달됨

주의:

- 광고, 마케팅, 제3자 판매 목적으로 쓰지 않는다고 답한다.

### User-generated content

수집 또는 처리:

- 화면 위 낙서 stroke와 snapshot
- 러브노트
- 데이트 플랜, 버킷리스트
- 추억 제목/메모
- 퀴즈 답변, Daily Spark 답변
- Discussion 메시지

용도:

- App functionality
- Account management

공유:

- 같은 방의 참여자에게 Firebase Realtime Database/Storage를 통해 동기화
- 사용자가 직접 Android 공유 메뉴를 호출한 경우 선택한 공유 앱으로 전달

주의:

- 방 참여자 외 공개 피드나 추천/광고 목적으로 사용하지 않는다고 답한다.

### Photos and videos

수집 또는 처리:

- 사용자가 Android Photo Picker로 직접 선택한 이미지
- 낙서 snapshot 이미지
- 추억/퀴즈/Discussion 첨부 이미지

용도:

- App functionality

공유:

- 같은 방 참여자에게 Firebase Storage를 통해 표시
- 사용자가 직접 공유/내보내기를 선택한 경우 해당 공유 대상 앱으로 전달

주의:

- 전체 갤러리 접근 권한을 요청하지 않는다.
- 사용자가 고른 이미지 외 사진을 자동 스캔하지 않는다.

### Location

수집 또는 처리:

- Approximate location
- Precise location

조건:

- Distance Widget을 사용자가 직접 켠 경우
- 양쪽 참여자가 모두 동의한 경우
- 사용자가 위치 공유 버튼을 눌렀을 때의 1회 위치값

용도:

- App functionality

공유:

- 같은 방의 상대방에게 거리 표시 목적으로만 동기화

주의:

- 기본값은 OFF
- 백그라운드 지속 위치 수집 없음
- 몰래 추적 없음
- 연결이 끊긴 뒤에는 마지막 공유 기준이라고 표시

### App activity

별도 분석 SDK나 광고 SDK를 넣지 않는 현재 구조에서는 Analytics 항목을 수집한다고 답하지 않는다.

단, Firebase Realtime Database 연결 상태, 방 상태, updatedAt, lastSeen처럼 앱 기능을 위해 저장되는 상태값은 해당 기능 데이터와 함께 개인정보 처리방침에 설명한다.

### Device or other IDs

현재 출시 방향은 Google OAuth/Firebase uid 기반이다. Android Advertising ID, IMEI, IMSI, SIM serial, 설치 앱 목록 같은 민감한 기기 식별자는 사용하지 않는다고 답한다.

## 보안 관행

- 데이터 전송 암호화: Firebase SDK의 TLS 전송 사용
- 삭제 요청: `LOVEDRAW_ACCOUNT_DELETION_URL`에 공개 웹 경로를 게시하고, `LOVEDRAW_SUPPORT_EMAIL`로 접수 가능한 연락 경로를 제공해야 함
- 삭제 처리 기간: 접수 후 `LOVEDRAW_DELETION_PROCESSING_PERIOD` 내 처리한다고 안내
- 독립 보안 검토: 실제 보안 검토를 받지 않았다면 아니오

## Play Console에 쓰기 좋은 요약 문구

```text
lovedraw collects Google account profile information, room data, user-created drawings, notes, plans, quiz answers, selected images, and optional one-time location data to provide real-time room sync, overlay drawing, widgets, and distance features between room members. The app does not sell user data, does not use advertising or analytics SDKs, and does not collect location in the background.
```

## 제출 전 필수 대조

- [docs/privacy-policy-ko.md](./privacy-policy-ko.md)의 수집 항목과 일치
- 앱 첫 실행 권한 안내와 일치
- Play Console 권한 선언과 일치
- Firebase Database/Storage rules가 방 참여자 범위로 제한됨
- 실제 운영자명, 문의 이메일, 개인정보 처리방침 공개 URL 입력 완료
- 계정/데이터 삭제 요청 경로 공개 완료
