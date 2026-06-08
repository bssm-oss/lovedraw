package com.example.couplecanvas.presentation.screen.waiting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.couplecanvas.presentation.component.BrandIconTile
import com.example.couplecanvas.presentation.component.CuteCodeCard
import com.example.couplecanvas.presentation.component.InviteShareDialog
import com.example.couplecanvas.presentation.component.RoundedPastelButton
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.Coral
import com.example.couplecanvas.presentation.theme.Mint
import com.example.couplecanvas.presentation.theme.RauschPink
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.SunshineYellowDeep
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmSurface
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.util.ConnectionDisplayState
import com.example.couplecanvas.util.WaitingInviteCopy
import com.example.couplecanvas.util.connectionDisplayState

@Composable
fun WaitingRoomScreen(roomId: String, onBack: () -> Unit, onOpenRoom: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WaitingRoomViewModel = viewModel(
        key = "waiting-$roomId",
        factory = ViewModelFactory { WaitingRoomViewModel(roomId, container.authRepository, container.roomRepository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.partnerJoined) {
        if (uiState.partnerJoined) onOpenRoom(roomId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 상단 30% — 노란 배경
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.35f)
                .background(SunshineYellow),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandIconTile(Modifier.size(80.dp))
                Text(
                    "상대방을 기다리는 중",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmBlack,
                    textAlign = TextAlign.Center,
                )
                if (!uiState.isFirebaseConnected) {
                    Text(
                        "재연결 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = Coral,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // 하단 카드 — 흰 배경, 상단 라운드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxSize(0.68f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val room = uiState.room
                if (room != null) {
                    CuteCodeCard(
                        roomCode = room.roomCode,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Couple Canvas Code", room.roomCode))
                            Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    WaitingConnectionStatusCard(
                        status = room.connectionDisplayState(uiState.isFirebaseConnected),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RoundedPastelButton(
                        text = WaitingInviteCopy.PRIMARY_BUTTON,
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        WaitingInviteCopy.HELPER,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = SunshineYellow,
                    trackColor = SunshineYellow.copy(alpha = 0.25f),
                )
                Spacer(Modifier.height(4.dp))
                SecondaryPastelButton(
                    text = "방 나가기",
                    onClick = {
                        viewModel.leave()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    val room = uiState.room
    if (showInviteDialog && room != null) {
        InviteShareDialog(
            roomCode = room.roomCode,
            roomTitle = room.title,
            onDismiss = { showInviteDialog = false },
            onMessage = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() },
        )
    }
}

@Composable
private fun WaitingConnectionStatusCard(status: ConnectionDisplayState, modifier: Modifier = Modifier) {
    val accent = when (status) {
        ConnectionDisplayState.Connected -> Mint
        ConnectionDisplayState.Waiting -> SunshineYellowDeep
        ConnectionDisplayState.Reconnecting -> RauschPink
        ConnectionDisplayState.Archived -> WarmGray
    }
    Row(
        modifier = modifier
            .background(WarmSurface, RoundedCornerShape(18.dp))
            .border(1.dp, Sand, RoundedCornerShape(18.dp))
            .semantics { contentDescription = status.accessibilityLabel }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accent, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(status.label, style = MaterialTheme.typography.labelLarge, color = WarmBlack)
            Text(status.description, style = MaterialTheme.typography.bodySmall, color = WarmGray)
        }
    }
}
