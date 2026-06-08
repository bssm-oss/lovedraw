package com.example.couplecanvas.presentation.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SoftPink
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.SunshineYellowDeep
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface
import com.example.couplecanvas.presentation.theme.WarmSurfaceAlt
import com.example.couplecanvas.util.LegalLinksCardCopy
import com.example.couplecanvas.util.ReleaseLegalConfig

@Composable
fun RoundedPastelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = SunshineYellow,
    contentColor: Color = WarmBlack,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryPastelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = WarmSurface, contentColor = WarmBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, Sand),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LegalLinksCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val legalLinks = ReleaseLegalConfig.current()
    SoftCard(modifier) {
        SectionTitle(LegalLinksCardCopy.TITLE)
        Text(
            if (legalLinks.isReleaseReady) {
                LegalLinksCardCopy.READY_BODY
            } else {
                LegalLinksCardCopy.NOT_READY_BODY
            },
            color = WarmGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryPastelButton(
                LegalLinksCardCopy.PRIVACY_BUTTON,
                onClick = { context.openWebUrl(legalLinks.privacyPolicyUrl) },
                modifier = Modifier.weight(1f),
                enabled = legalLinks.hasPrivacyPolicyUrl,
            )
            SecondaryPastelButton(
                LegalLinksCardCopy.DELETION_BUTTON,
                onClick = { context.openWebUrl(legalLinks.accountDeletionUrl) },
                modifier = Modifier.weight(1f),
                enabled = legalLinks.hasAccountDeletionUrl,
            )
        }
        SecondaryPastelButton(
            LegalLinksCardCopy.SUPPORT_BUTTON,
            onClick = { context.sendSupportEmail(legalLinks.supportEmail) },
            modifier = Modifier.fillMaxWidth(),
            enabled = legalLinks.hasSupportEmail,
        )
    }
}

@Composable
fun CuteTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.background(WarmSurface, CircleShape)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기", tint = WarmBlack)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = WarmGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}

private fun Context.openWebUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Context.sendSupportEmail(email: String) {
    runCatching {
        val uri = Uri.parse("mailto:$email")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "lovedraw 문의")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
    )
}

@Composable
fun CanvasHeartMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shadow = Path().apply {
            moveTo(w * 0.26f, h * 0.78f)
            cubicTo(w * 0.38f, h * 0.90f, w * 0.68f, h * 0.88f, w * 0.76f, h * 0.75f)
            cubicTo(w * 0.66f, h * 0.82f, w * 0.42f, h * 0.84f, w * 0.26f, h * 0.78f)
            close()
        }
        drawPath(shadow, SunshineYellow.copy(alpha = 0.28f), style = Fill)

        val yellowStroke = Path().apply {
            moveTo(w * 0.46f, h * 0.72f)
            cubicTo(w * 0.26f, h * 0.56f, w * 0.22f, h * 0.36f, w * 0.36f, h * 0.27f)
            cubicTo(w * 0.48f, h * 0.19f, w * 0.58f, h * 0.29f, w * 0.54f, h * 0.44f)
            cubicTo(w * 0.52f, h * 0.54f, w * 0.48f, h * 0.62f, w * 0.46f, h * 0.72f)
        }
        drawPath(
            path = yellowStroke,
            color = SunshineYellow,
            style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val pinkStroke = Path().apply {
            moveTo(w * 0.56f, h * 0.43f)
            cubicTo(w * 0.60f, h * 0.27f, w * 0.78f, h * 0.24f, w * 0.84f, h * 0.39f)
            cubicTo(w * 0.94f, h * 0.62f, w * 0.66f, h * 0.75f, w * 0.46f, h * 0.78f)
        }
        drawPath(
            path = pinkStroke,
            color = SoftPink,
            style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val yellowHighlight = Path().apply {
            moveTo(w * 0.34f, h * 0.36f)
            cubicTo(w * 0.38f, h * 0.31f, w * 0.44f, h * 0.30f, w * 0.48f, h * 0.32f)
        }
        drawPath(
            path = yellowHighlight,
            color = Color.White,
            style = Stroke(width = w * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val pinkHighlight = Path().apply {
            moveTo(w * 0.72f, h * 0.34f)
            cubicTo(w * 0.77f, h * 0.33f, w * 0.81f, h * 0.36f, w * 0.82f, h * 0.40f)
        }
        drawPath(
            path = pinkHighlight,
            color = Color.White,
            style = Stroke(width = w * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val sparkle = Path().apply {
            moveTo(w * 0.80f, h * 0.66f)
            lineTo(w * 0.84f, h * 0.76f)
            lineTo(w * 0.94f, h * 0.80f)
            lineTo(w * 0.84f, h * 0.84f)
            lineTo(w * 0.80f, h * 0.94f)
            lineTo(w * 0.76f, h * 0.84f)
            lineTo(w * 0.66f, h * 0.80f)
            lineTo(w * 0.76f, h * 0.76f)
            close()
        }
        drawPath(sparkle, SunshineYellow)
        drawCircle(SoftPink, radius = w * 0.04f, center = Offset(w * 0.70f, h * 0.92f))
    }
}

@Composable
fun BrandIconTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(WarmSurface)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        CanvasHeartMark(Modifier.fillMaxSize())
    }
}

@Composable
fun CuteCodeCard(roomCode: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("초대 코드", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
                Text(roomCode, style = MaterialTheme.typography.displaySmall, color = WarmBlack, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onCopy, modifier = Modifier.background(SunshineYellow, CircleShape)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "코드 복사", tint = WarmBlack)
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrandIconTile(Modifier.size(60.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = WarmGray)
        }
    }
}

@Composable
fun FilterChipLike(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) SunshineYellow else WarmSurface
    val fg = WarmBlack
    val borderColor = if (selected) SunshineYellowDeep else Sand
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun ConnectionPill(connected: Boolean, modifier: Modifier = Modifier) {
    if (connected) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFECE7))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "재연결 중",
            color = Color(0xFFBC4B35),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = SunshineYellowDeep, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun Spacer8() {
    Spacer(Modifier.height(8.dp))
}
