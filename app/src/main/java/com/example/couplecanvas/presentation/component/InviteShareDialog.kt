package com.example.couplecanvas.presentation.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmCanvas
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface
import com.example.couplecanvas.util.InviteShareHelper

@Composable
fun InviteShareDialog(
    roomCode: String,
    roomTitle: String?,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val invite = remember(roomCode, roomTitle) { InviteShareHelper.payload(roomCode, roomTitle) }
    val qrBitmap = remember(invite.inviteLink) { InviteShareHelper.createQrBitmap(invite.inviteLink) }

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        onMessage("복사했어요")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WarmSurface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("초대하기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("초대 코드", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
                    Text(
                        roomCode,
                        style = MaterialTheme.typography.displaySmall,
                        color = WarmBlack,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .background(WarmCanvas, RoundedCornerShape(24.dp))
                        .border(1.dp, Sand, RoundedCornerShape(24.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(qrBitmap.asImageBitmap(), contentDescription = "초대 QR 코드", modifier = Modifier.size(156.dp))
                }
                Text(
                    "QR을 보여주거나 링크를 보내면 바로 입장할 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryPastelButton(
                        text = "카톡",
                        onClick = {
                            if (!InviteShareHelper.shareToKakaoTalk(context, invite)) {
                                InviteShareHelper.shareGeneric(context, invite)
                                onMessage("카카오톡이 없어서 공유 메뉴를 열었어요")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryPastelButton(
                        text = "인스타 DM",
                        onClick = {
                            if (!InviteShareHelper.shareToInstagram(context, invite)) {
                                InviteShareHelper.shareGeneric(context, invite)
                                onMessage("인스타그램이 없어서 공유 메뉴를 열었어요")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundedPastelButton(
                        text = "공유",
                        onClick = { InviteShareHelper.shareGeneric(context, invite) },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryPastelButton(
                        text = "복사",
                        onClick = { copy("lovedraw invite", invite.shareText) },
                        modifier = Modifier.weight(1f),
                    )
                }
                ShareMiniHint(onCopyCode = { copy("lovedraw code", roomCode) })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun ShareMiniHint(onCopyCode: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SunshineYellow.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = WarmBlack, modifier = Modifier.size(20.dp))
        Text("상대가 링크를 못 열면 코드만 입력해도 돼요.", style = MaterialTheme.typography.bodySmall, color = WarmBlack, modifier = Modifier.weight(1f))
        TextButton(onClick = onCopyCode) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("코드")
        }
        Icon(Icons.Rounded.IosShare, contentDescription = null, tint = WarmGray, modifier = Modifier.size(18.dp))
    }
}
