# lovedraw Foreground Service Special Use 선언 초안

이 문서는 Play Console > App content > Foreground service permissions 또는 Android 14 이상 foreground service 관련 선언을 작성할 때 참고하는 초안이다.

## 사용 권한

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- Service type: `specialUse`
- Manifest subtype:

```text
User-enabled Couple Canvas drawing overlay shown over other apps so the user can draw directly on the screen and sync strokes with their selected room.
```

## Play Console 설명 문구

```text
lovedraw uses a user-enabled foreground service to show a drawing overlay over other apps. This lets the user draw directly on the current screen and sync short strokes with the selected lovedraw room. The service is visible through a persistent notification with drawing controls, and the user can stop it at any time from the notification, overlay controls, or inside the app.
```

## 사용자에게 보이는 기능 흐름

1. 사용자가 Google 계정으로 로그인한다.
2. 사용자가 방을 만들거나 초대 코드/링크/QR로 방에 입장한다.
3. 앱이 알림 권한과 `다른 앱 위에 표시` 권한이 필요한 이유를 먼저 설명한다.
4. 사용자가 권한을 허용한다.
5. 사용자가 앱 또는 알림에서 그리기를 시작한다.
6. foreground service가 지속 알림과 함께 실행된다.
7. 사용자가 현재 보고 있는 화면 위에 직접 낙서한다.
8. stroke는 Firebase를 통해 선택한 방의 상대에게 동기화된다.
9. 사용자가 알림/오버레이/앱에서 그리기를 끄거나 전체 지우기를 실행할 수 있다.

## 앱 내부 권한 안내 기준 문구

Play Console 설명, 개인정보 처리방침, Data safety 답변, 심사용 영상 설명은 앱 내부 첫 실행 안내와 같은 내용을 말해야 한다.

```text
상대에게 낙서를 보내려면 처음 한 번만 권한이 필요해요. 알림은 시작과 끄기에, 다른 앱 위에 표시는 지금 보는 화면 위 선에만 사용해요.
```

```text
상대 화면에 낙서를 보내기 위해 필요해요. lovedraw는 화면 내용을 읽거나 저장하지 않아요.
```

## 정책상 강조해야 할 점

- 사용자가 직접 켠 경우에만 실행
- 지속 알림 표시
- 화면 내용을 캡처하지 않음
- 다른 앱의 텍스트, 이미지, 키 입력을 읽지 않음
- Accessibility Service를 사용하지 않음
- 광고, 분석, 몰래 추적 목적으로 동작하지 않음
- 사용자가 언제든 끌 수 있음

## 심사용 데모 영상에 포함할 장면

- 첫 실행 권한 안내
- 알림 권한 요청
- 다른 앱 위 표시 설정 화면 이동
- 방 선택 후 그리기 시작
- 홈 화면 또는 다른 앱 위에 overlay 표시
- 상대 화면에 stroke 동기화
- 알림에서 그리기 끄기
- 전체 지우기 또는 fade-out 동작

## 제출 전 확인

- 앱 내부 권한 안내 문구와 이 설명이 같은 내용을 말하는지 확인
- `AndroidManifest.xml`의 special use subtype이 실제 기능과 일치하는지 확인
- release 빌드에서 foreground service 알림이 항상 보이는지 확인
- 실제 기기에서 권한 거부/허용/중지 흐름을 녹화해 보관
