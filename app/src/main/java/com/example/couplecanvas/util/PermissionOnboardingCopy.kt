package com.example.couplecanvas.util

object PermissionOnboardingCopy {
    const val HEADER_TITLE = "화면 위에 마음을 보내려면"
    const val HEADER_BODY = "상대에게 낙서를 보내려면 알림과 화면 위 그리기 권한이 필요해요. lovedraw는 화면 내용을 읽거나 몰래 저장하지 않아요."

    const val NOTIFICATION_TITLE = "알림"
    const val NOTIFICATION_BODY = "그리기 시작, 끄기, 전체 지우기를 알림에서 바로 조작해요."
    const val NOTIFICATION_DENIED = "알림 권한을 허용해주세요"

    const val OVERLAY_TITLE = "화면 위 그리기"
    const val OVERLAY_BODY = "상대에게 낙서를 보내려면 지금 보는 화면 위에 선을 띄울 수 있어야 해요."
    const val OVERLAY_DENIED = "화면 위 그리기를 허용해주세요"

    const val LOCATION_TITLE = "위치"
    const val LOCATION_BODY = "거리 위젯을 직접 켤 때만 사용해요. 기본값은 꺼짐이에요."
    const val LOCATION_DENIED = "거리 위젯은 위치 권한이 필요해요"

    const val SAFETY_TITLE = "권한은 이 기능에만 사용해요"
    const val SAFETY_BODY = "화면 캡처 없음 · 몰래 위치 추적 없음 · 언제든 설정에서 끌 수 있음"

    const val REQUIRED_PERMISSION_DENIED = "필수 권한을 먼저 허용해주세요"
    const val PRIMARY_BUTTON_IDLE = "권한 설정하기"
    const val PRIMARY_BUTTON_PENDING = "확인 중..."
    const val SECONDARY_BUTTON = "다시 확인"
}
