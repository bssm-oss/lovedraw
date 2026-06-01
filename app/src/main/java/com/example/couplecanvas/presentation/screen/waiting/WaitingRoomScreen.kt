package com.example.couplecanvas.presentation.screen.waiting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.couplecanvas.presentation.component.BrandIconTile
import com.example.couplecanvas.presentation.component.CuteCodeCard
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.Coral
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.WarmBlack

@Composable
fun WaitingRoomScreen(roomId: String, onBack: () -> Unit, onOpenRoom: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WaitingRoomViewModel = viewModel(
        key = "waiting-$roomId",
        factory = ViewModelFactory { WaitingRoomViewModel(roomId, container.authRepository, container.roomRepository) },
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                        },
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
}
