package com.example.couplecanvas.presentation.screen.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.couplecanvas.BuildConfig
import com.example.couplecanvas.presentation.component.BrandIconTile
import com.example.couplecanvas.presentation.component.LegalLinksCard
import com.example.couplecanvas.presentation.component.RoundedPastelButton
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.Coral
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurfaceAlt
import com.example.couplecanvas.util.LoginLegalConsentCopy
import com.example.couplecanvas.util.ReleaseLegalConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import java.security.SecureRandom

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(container.authRepository) })
    val user by viewModel.user.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val webClientId = remember(resources, context.packageName) {
        val id = resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id != 0) resources.getString(id) else ""
    }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var credentialFlowLoading by remember { mutableStateOf(false) }
    var legalConsentAccepted by remember { mutableStateOf(false) }
    val legalLinks = remember { ReleaseLegalConfig.current() }
    val legalConsentLinksReady = BuildConfig.DEBUG || legalLinks.hasRequiredConsentLinks
    val isLoading = uiState.isLoading || credentialFlowLoading

    LaunchedEffect(user) {
        if (user != null) onSignedIn()
    }

    val signInClick: () -> Unit = {
        val activity = context.findActivity()
        if (activity == null) {
            viewModel.showError("다시 실행해주세요")
        } else {
            coroutineScope.launch {
                credentialFlowLoading = true
                runCatching {
                    requestGoogleIdToken(
                        credentialManager = credentialManager,
                        activity = activity,
                        webClientId = webClientId,
                    )
                }.onSuccess { token ->
                    credentialFlowLoading = false
                    viewModel.signInWithGoogleToken(token)
                }.onFailure { error ->
                    credentialFlowLoading = false
                    viewModel.showError(error.toLoginMessage())
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 상단 45% — 노란 배경
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.45f)
                .background(SunshineYellow),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandIconTile(Modifier.size(96.dp))
                Text(
                    "lovedraw",
                    style = MaterialTheme.typography.displaySmall,
                    color = WarmBlack,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 하단 카드 — 흰 배경, 상단 라운드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxSize(0.58f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .padding(horizontal = 32.dp, vertical = 36.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "함께 그리는\n우리만의 캔버스",
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmBlack,
                )
                Spacer(Modifier.height(8.dp))
                LegalLinksCard(Modifier.fillMaxWidth())
                LoginLegalConsentRow(
                    checked = legalConsentAccepted,
                    onCheckedChange = { legalConsentAccepted = it },
                )
                RoundedPastelButton(
                    text = if (isLoading) "로그인 중..." else "Google로 시작하기",
                    enabled = !isLoading && webClientId.isNotBlank() && legalConsentAccepted && legalConsentLinksReady,
                    onClick = signInClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!legalConsentAccepted) {
                    Text(LoginLegalConsentCopy.REQUIRED, color = WarmGray, style = MaterialTheme.typography.bodySmall)
                }
                if (!legalConsentLinksReady) {
                    Text(LoginLegalConsentCopy.LINKS_REQUIRED, color = Coral, style = MaterialTheme.typography.bodySmall)
                }
                if (webClientId.isBlank()) {
                    Text("Google 설정 필요", color = Coral, style = MaterialTheme.typography.bodySmall)
                }
                if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
                    SecondaryDebugLoginButton(enabled = !isLoading, onClick = viewModel::signInForDebugTest)
                }
                uiState.error?.let { Text(it, color = Coral, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun LoginLegalConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WarmSurfaceAlt.copy(alpha = 0.72f))
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = SunshineYellow,
                checkmarkColor = WarmBlack,
                uncheckedColor = WarmGray,
            ),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(LoginLegalConsentCopy.TITLE, style = MaterialTheme.typography.labelLarge, color = WarmBlack)
            Text(LoginLegalConsentCopy.BODY, style = MaterialTheme.typography.bodySmall, color = WarmGray)
            Text(LoginLegalConsentCopy.CHECKBOX, style = MaterialTheme.typography.bodySmall, color = WarmBlack)
        }
    }
}

@Composable
private fun SecondaryDebugLoginButton(enabled: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    SecondaryPastelButton(
        text = "개발 테스트 로그인",
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "디버그 전용",
        style = MaterialTheme.typography.bodySmall,
        color = WarmGray,
    )
}

private suspend fun requestGoogleIdToken(
    credentialManager: CredentialManager,
    activity: Activity,
    webClientId: String,
): String {
    val credential = try {
        credentialManager.getCredential(
            context = activity,
            request = googleCredentialRequest(webClientId, filterAuthorizedAccounts = true),
        ).credential
    } catch (error: NoCredentialException) {
        credentialManager.getCredential(
            context = activity,
            request = googleCredentialRequest(webClientId, filterAuthorizedAccounts = false),
        ).credential
    }

    val customCredential = credential as? CustomCredential
        ?: error("지원하지 않는 Google 로그인 응답이에요.")

    if (customCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        error("지원하지 않는 Google 로그인 응답이에요.")
    }

    return try {
        GoogleIdTokenCredential.createFrom(customCredential.data).idToken
    } catch (error: GoogleIdTokenParsingException) {
        throw IllegalStateException("Google 로그인 응답을 확인할 수 없어요.", error)
    }
}

private fun googleCredentialRequest(
    webClientId: String,
    filterAuthorizedAccounts: Boolean,
): GetCredentialRequest {
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
        .setAutoSelectEnabled(filterAuthorizedAccounts)
        .setNonce(generateSecureRandomNonce())
        .build()
    return GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()
}

private fun generateSecureRandomNonce(byteLength: Int = 32): String {
    val randomBytes = ByteArray(byteLength)
    SecureRandom().nextBytes(randomBytes)
    return Base64.encodeToString(randomBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Throwable.toLoginMessage(): String =
    when (this) {
        is NoCredentialException -> "Google 계정 없음"
        else -> message ?: "로그인 실패"
    }
