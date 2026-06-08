package com.example.couplecanvas.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class InviteSharePayload(
    val roomCode: String,
    val roomTitle: String,
) {
    val inviteLink: String = "lovedraw://invite?code=$roomCode"
    val shareText: String =
        "$roomTitle 초대장이에요.\n\nlovedraw에서 코드 $roomCode 로 들어와 주세요.\n$inviteLink"
}

object InviteShareHelper {
    private const val KAKAO_TALK_PACKAGE = "com.kakao.talk"
    private const val INSTAGRAM_PACKAGE = "com.instagram.android"

    fun payload(roomCode: String, roomTitle: String?): InviteSharePayload =
        InviteSharePayload(
            roomCode = roomCode.trim().uppercase(),
            roomTitle = roomTitle?.takeIf { it.isNotBlank() } ?: "lovedraw",
        )

    fun shareToKakaoTalk(context: Context, payload: InviteSharePayload): Boolean =
        shareToPackage(context, payload, KAKAO_TALK_PACKAGE)

    fun shareToInstagram(context: Context, payload: InviteSharePayload): Boolean =
        shareToPackage(context, payload, INSTAGRAM_PACKAGE)

    fun shareGeneric(context: Context, payload: InviteSharePayload) {
        val chooser = Intent.createChooser(payload.sendIntent(), "초대장 보내기")
        context.startActivity(chooser)
    }

    fun createQrBitmap(content: String, sizePx: Int = 720): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.rgb(38, 35, 32) else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun shareToPackage(context: Context, payload: InviteSharePayload, packageName: String): Boolean {
        val intent = payload.sendIntent().setPackage(packageName)
        val canHandle = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        if (!canHandle) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse { error ->
            if (error is ActivityNotFoundException) false else throw error
        }
    }

    private fun InviteSharePayload.sendIntent(): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, "$roomTitle 초대")
            clipData = android.content.ClipData.newPlainText("lovedraw invite", inviteLink)
            data = Uri.parse(inviteLink)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
