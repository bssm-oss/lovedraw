package com.example.couplecanvas.presentation.screen.room

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.couplecanvas.data.model.BucketItem
import com.example.couplecanvas.data.model.BucketStatus
import com.example.couplecanvas.data.model.DateMode
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DateVote
import com.example.couplecanvas.data.model.LoveNote
import com.example.couplecanvas.data.model.MemoryItem
import com.example.couplecanvas.data.model.ModelFactory
import com.example.couplecanvas.data.model.QuizAnswer
import com.example.couplecanvas.data.model.QuizDiscussion
import com.example.couplecanvas.data.model.QuizQuestion
import com.example.couplecanvas.presentation.component.CuteTopBar
import com.example.couplecanvas.presentation.component.EmptyState
import com.example.couplecanvas.presentation.component.FilterChipLike
import com.example.couplecanvas.presentation.component.InviteShareDialog
import com.example.couplecanvas.presentation.component.RoundedPastelButton
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.component.SectionTitle
import com.example.couplecanvas.presentation.component.SoftCard
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.screen.drawing.DrawingScreen
import com.example.couplecanvas.presentation.theme.RauschPink
import com.example.couplecanvas.presentation.theme.Mint
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.SunshineYellowDeep
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmCanvas
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface
import com.example.couplecanvas.util.ConnectionDisplayState
import com.example.couplecanvas.util.DateIdeaGenerator
import com.example.couplecanvas.util.DatePlanMatcher
import com.example.couplecanvas.util.ReleaseLegalConfig
import com.example.couplecanvas.util.ShareCardGenerator
import com.example.couplecanvas.util.StatsCalculator
import com.example.couplecanvas.util.connectionDisplayState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val moreTabs = listOf("퀴즈", "Spark", "버킷", "설정")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

@Composable
fun RoomDashboardScreen(roomId: String, initialTab: Int = 0, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: RoomFeatureViewModel = viewModel(
        key = "features-$roomId",
        factory = ViewModelFactory {
            RoomFeatureViewModel(roomId, container.authRepository, container.roomRepository, container.featureRepository, container.widgetStateStore)
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNavItem by remember(roomId, initialTab) { mutableIntStateOf(if (initialTab <= 3) initialTab else 4) }
    var selectedMoreSubTab by remember(roomId, initialTab) { mutableIntStateOf((initialTab - 4).coerceAtLeast(0)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val room = uiState.room
    val feedback = uiState.error ?: uiState.message
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        containerColor = WarmCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = WarmSurface) {
                val navLabels = listOf("그리기", "노트", "데이트", "추억", "더보기")
                val navIcons = listOf(Icons.Rounded.Brush, Icons.AutoMirrored.Rounded.Chat, Icons.Rounded.Favorite, Icons.Rounded.PhotoAlbum, Icons.Rounded.MoreHoriz)
                navLabels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedNavItem == index,
                        onClick = { selectedNavItem = index },
                        icon = { Icon(navIcons[index], contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = SunshineYellow),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().background(WarmCanvas).padding(innerPadding)) {
            CuteTopBar(
                title = room?.title ?: "그림방",
                subtitle = room?.roomCode?.let {
                    "코드 $it"
                },
                onBack = onBack,
                action = {
                    if (!room?.roomCode.isNullOrBlank()) {
                        IconButton(
                            onClick = { showInviteDialog = true },
                            modifier = Modifier.background(WarmSurface, RoundedCornerShape(16.dp)),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "초대하기", tint = WarmBlack)
                        }
                    }
                },
            )
            room?.let {
                RoomConnectionBanner(
                    status = it.connectionDisplayState(uiState.isFirebaseConnected),
                    roomCode = it.roomCode,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            when (selectedNavItem) {
                0 -> DrawingScreen(
                    roomId = roomId,
                    roomCode = room?.roomCode,
                    roomTitle = room?.title,
                    privacyMode = room?.privacyMode ?: false,
                )
                1 -> NotesTab(uiState, viewModel)
                2 -> DatePlannerTab(uiState, viewModel)
                3 -> MemoriesTab(uiState, viewModel)
                4 -> MoreTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    selectedSubTab = selectedMoreSubTab,
                    onSubTabSelected = { selectedMoreSubTab = it },
                )
            }
        }
    }

    if (showInviteDialog && room != null) {
        InviteShareDialog(
            roomCode = room.roomCode,
            roomTitle = room.title,
            onDismiss = { showInviteDialog = false },
            onMessage = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
        )
    }
}

@Composable
private fun RoomConnectionBanner(status: ConnectionDisplayState, roomCode: String, modifier: Modifier = Modifier) {
    val accent = when (status) {
        ConnectionDisplayState.Connected -> Mint
        ConnectionDisplayState.Waiting -> SunshineYellowDeep
        ConnectionDisplayState.Reconnecting -> RauschPink
        ConnectionDisplayState.Archived -> WarmGray
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WarmSurface, RoundedCornerShape(18.dp))
            .border(1.dp, Sand, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(accent, RoundedCornerShape(999.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(status.label, style = MaterialTheme.typography.labelLarge, color = WarmBlack)
            Text(status.description, style = MaterialTheme.typography.bodySmall, color = WarmGray)
        }
        Text(roomCode, style = MaterialTheme.typography.labelLarge, color = accent)
    }
}

@Composable
private fun MoreTab(
    uiState: RoomFeatureUiState,
    viewModel: RoomFeatureViewModel,
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedSubTab, edgePadding = 16.dp) {
            moreTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { onSubTabSelected(index) },
                    text = { Text(title) },
                )
            }
        }
        when (selectedSubTab) {
            0 -> QuizzesTab(uiState, viewModel)
            1 -> DailySparkTab(uiState, viewModel)
            2 -> BucketTab(uiState, viewModel)
            3 -> StatsTab(uiState, viewModel)
        }
    }
}

@Composable
private fun TabScaffold(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WarmCanvas).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun NotesTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var message by remember { mutableStateOf("") }
    val unreadReceivedNoteIds = uiState.notes
        .filter { it.authorUid != uiState.localUid && !it.isRead }
        .joinToString(separator = "|") { it.noteId }
    LaunchedEffect(unreadReceivedNoteIds) {
        if (unreadReceivedNoteIds.isNotBlank()) {
            viewModel.markReceivedNotesRead()
        }
    }
    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Sticky Love Notes")
                OutlinedTextField(value = message, onValueChange = { message = it.take(120) }, label = { Text("짧은 메시지") }, modifier = Modifier.fillMaxWidth())
                RoundedPastelButton("보내기", onClick = { viewModel.sendNote(message); message = "" }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (uiState.notes.isEmpty()) item { EmptyState("노트 없음", "비어 있음") }
        items(uiState.notes, key = { it.noteId }) { note ->
            SoftCard(Modifier.fillMaxWidth()) {
                Text(if (note.isPinned) "고정된 노트" else "러브노트", color = RauschPink, style = MaterialTheme.typography.labelLarge)
                Text(note.message, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditNoteButton(
                        note = note,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.weight(1f),
                        onSave = { viewModel.updateNote(note.noteId, it) },
                    )
                    SecondaryPastelButton(if (note.isPinned) "고정 해제" else "고정", onClick = { viewModel.toggleNotePin(note) }, modifier = Modifier.weight(1f))
                    DeleteRecordButton(
                        label = "삭제",
                        title = "노트 삭제",
                        message = "삭제할까요?",
                        enabled = !uiState.isBusy,
                        modifier = Modifier.weight(1f),
                        onConfirm = { viewModel.deleteNote(note.noteId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DatePlannerTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var mode by remember { mutableStateOf(DateMode.Nearby) }
    var vibe by remember { mutableStateOf("Cozy") }
    var budget by remember { mutableStateOf("보통") }
    var time by remember { mutableStateOf("1시간") }
    var customTitle by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }
    var customDateText by remember { mutableStateOf(LocalDate.now().plusDays(3).format(dateFormatter)) }
    var customSteps by remember { mutableStateOf("") }
    var customDateError by remember { mutableStateOf<String?>(null) }
    var clearCustomAfterSave by remember { mutableStateOf(false) }
    val matchedPlans = uiState.datePlans.filter { DatePlanMatcher.isMatched(it, uiState.room) }
    val voteDeckPlans = uiState.datePlans.filter {
        it.status == DatePlanStatus.Saved.value &&
            !DatePlanMatcher.isMatched(it, uiState.room) &&
            it.votes[uiState.localUid] == null
    }
    val upcomingPlans = uiState.datePlans.filter { it.status == DatePlanStatus.Upcoming.value }
    val savedPlans = uiState.datePlans.filter { it.status == DatePlanStatus.Saved.value && it !in matchedPlans }
    val pastPlans = uiState.datePlans.filter { it.status == DatePlanStatus.Past.value }
    val nextDateText = StatsCalculator.nextDateCountdownText(uiState.datePlans)

    LaunchedEffect(uiState.message) {
        if (clearCustomAfterSave && uiState.message == "데이트 플랜을 저장했어요") {
            customTitle = ""
            customDescription = ""
            customSteps = ""
            customDateText = LocalDate.now().plusDays(3).format(dateFormatter)
            clearCustomAfterSave = false
        }
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) clearCustomAfterSave = false
    }

    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("데이트 플래너")
                Text("모드", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateMode.entries.forEach { FilterChipLike(it.displayLabel(), mode == it, onClick = { mode = it }) }
                }
                Text("분위기 휠", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateIdeaGenerator.vibes.forEach { option -> FilterChipLike(option.vibeLabel(), vibe == option, onClick = { vibe = option }) }
                }
                SecondaryPastelButton("랜덤 분위기 돌리기", onClick = { vibe = DateIdeaGenerator.randomVibe() }, modifier = Modifier.fillMaxWidth())
                Text("예산", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("낮음", "보통", "높음").forEach { option -> FilterChipLike(option, budget == option, onClick = { budget = option }) }
                }
                Text("시간", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("30분", "1시간", "반나절", "하루").forEach { option -> FilterChipLike(option, time == option, onClick = { time = option }) }
                }
                RoundedPastelButton(
                    "3개 만들기",
                    onClick = { viewModel.generateDateIdeas(mode, vibe, budget, time) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy,
                )
            }
        }
        item {
            DateVoteDeck(voteDeckPlans, uiState, viewModel)
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("D-Day 위젯")
                Text(nextDateText, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (uiState.widgetRoomId == uiState.room?.roomId) {
                        "대표방"
                    } else {
                        "대표방 아님"
                    },
                    color = WarmGray,
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("직접 일정 추가")
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it.take(60) },
                    label = { Text("데이트 제목") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = customDescription,
                    onValueChange = { customDescription = it.take(220) },
                    label = { Text("설명") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = customDateText,
                    onValueChange = {
                        customDateText = it.take(10)
                        customDateError = null
                    },
                    label = { Text("예정일 YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = customDateError != null,
                    supportingText = customDateError?.let { { Text(it) } },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it.take(24) },
                        label = { Text("예산") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it.take(24) },
                        label = { Text("시간") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = customSteps,
                    onValueChange = { customSteps = it.take(320) },
                    label = { Text("스텝, 줄마다 1개") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                RoundedPastelButton(
                    "내 데이트 저장",
                    onClick = {
                        val parsedDate = customDateText.toStartedAtMillis()
                        if (customDateText.isNotBlank() && parsedDate == null) {
                            customDateError = "날짜 형식을 확인해주세요"
                        } else {
                            clearCustomAfterSave = true
                            viewModel.addCustomDatePlan(customTitle, customDescription, mode, vibe, budget, time, customSteps, parsedDate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && customTitle.isNotBlank() && customDescription.isNotBlank(),
                )
            }
        }
        DatePlanSection("매칭", matchedPlans, uiState, viewModel, emptyMessage = "비어 있음")
        DatePlanSection("예정", upcomingPlans, uiState, viewModel, emptyMessage = "비어 있음")
        DatePlanSection("저장", savedPlans, uiState, viewModel, emptyMessage = "비어 있음")
        DatePlanSection("완료", pastPlans, uiState, viewModel, emptyMessage = "비어 있음")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateVoteDeck(plans: List<DatePlan>, uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    val pagerState = rememberPagerState(pageCount = { plans.size })
    val scope = rememberCoroutineScope()
    val safePage = pagerState.currentPage.coerceAtMost((plans.size - 1).coerceAtLeast(0))
    val currentPlan = plans.getOrNull(safePage)
    val deckSignature = plans.joinToString(separator = "|") { it.planId }

    LaunchedEffect(deckSignature) {
        if (plans.isNotEmpty() && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    fun voteCurrent(vote: DateVote) {
        val plan = currentPlan ?: return
        viewModel.votePlan(plan.planId, vote.value)
        val nextPage = (safePage + 1).coerceAtMost(plans.lastIndex)
        if (nextPage != safePage) {
            scope.launch { pagerState.animateScrollToPage(nextPage) }
        }
    }

    SoftCard(Modifier.fillMaxWidth()) {
        SectionTitle("아이디어 스와이프")
        if (plans.isEmpty()) {
            Text("투표할 플랜 없음", color = WarmGray)
        } else {
            Text("${safePage + 1}/${plans.size}", color = RauschPink, style = MaterialTheme.typography.labelLarge)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                key = { page -> plans[page].planId },
            ) { page ->
                val plan = plans[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(plan.tone.toneLabel(), color = RauschPink, style = MaterialTheme.typography.labelLarge)
                    Text(plan.title, style = MaterialTheme.typography.titleLarge)
                    Text(plan.description, color = WarmGray, maxLines = 3)
                    Text(
                        "${plan.vibe.vibeLabel()} · 예산 ${plan.estimatedBudget ?: "-"} · 시간 ${plan.estimatedTime ?: "-"}",
                        color = WarmGray,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    plan.steps.take(3).forEachIndexed { index, step ->
                        Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryPastelButton(
                    "별로",
                    onClick = { voteCurrent(DateVote.Nope) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isBusy && currentPlan != null,
                )
                SecondaryPastelButton(
                    "나중에",
                    onClick = { voteCurrent(DateVote.Later) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isBusy && currentPlan != null,
                )
                RoundedPastelButton(
                    "좋아요",
                    onClick = { voteCurrent(DateVote.Like) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isBusy && currentPlan != null,
                )
            }
        }
    }
}

private fun LazyListScope.DatePlanSection(
    title: String,
    plans: List<DatePlan>,
    uiState: RoomFeatureUiState,
    viewModel: RoomFeatureViewModel,
    emptyMessage: String,
) {
    item {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
    }
    if (plans.isEmpty()) {
        item { EmptyState(title, emptyMessage) }
    } else {
        items(plans, key = { "${title}-${it.planId}" }) { plan ->
            DatePlanCard(plan = plan, uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun DatePlanCard(plan: DatePlan, uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    val myVote = plan.votes[uiState.localUid]
    val likeCount = DatePlanMatcher.likedByMemberCount(plan, uiState.room)
    val matched = DatePlanMatcher.isMatched(plan, uiState.room)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shareError by remember(plan.planId) { mutableStateOf<String?>(null) }
    SoftCard(Modifier.fillMaxWidth()) {
        Text(if (matched) "매칭" else plan.tone.toneLabel(), color = RauschPink, style = MaterialTheme.typography.labelLarge)
        Text(plan.title, style = MaterialTheme.typography.titleMedium)
        Text(plan.description, color = WarmGray)
        Text(
            "${plan.scheduledAt.toDatePlanLabel()} · 예산 ${plan.estimatedBudget ?: "-"} · 시간 ${plan.estimatedTime ?: "-"} · 좋아요 $likeCount/2",
            color = WarmGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        plan.steps.forEachIndexed { index, step ->
            Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
        }
        if (matched) {
            Text("둘 다 좋아요", color = RauschPink)
        }
        EditDatePlanButton(
            plan = plan,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
            onSave = { title, description, vibe, budget, time, stepsText, scheduledAt ->
                viewModel.updateDatePlan(plan, title, description, vibe, budget, time, stepsText, scheduledAt)
            },
        )
        ScheduleDatePlanButton(
            plan = plan,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
            onSchedule = { scheduledAt -> viewModel.scheduleDatePlan(plan.planId, scheduledAt) },
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipLike("좋아요", myVote == DateVote.Like.value, { viewModel.votePlan(plan.planId, DateVote.Like.value) })
            FilterChipLike("별로", myVote == DateVote.Nope.value, { viewModel.votePlan(plan.planId, DateVote.Nope.value) })
            FilterChipLike("나중에", myVote == DateVote.Later.value, { viewModel.votePlan(plan.planId, DateVote.Later.value) })
            FilterChipLike("예정", plan.status == DatePlanStatus.Upcoming.value, { viewModel.updateDateStatus(plan.planId, DatePlanStatus.Upcoming) })
            FilterChipLike("저장", plan.status == DatePlanStatus.Saved.value, { viewModel.updateDateStatus(plan.planId, DatePlanStatus.Saved) })
            FilterChipLike("완료", plan.status == DatePlanStatus.Past.value, { viewModel.updateDateStatus(plan.planId, DatePlanStatus.Past) })
        }
        SecondaryPastelButton(
            "공유 카드",
            onClick = {
                shareError = null
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            ShareCardGenerator.createDatePlanCardUri(context, uiState.room?.title.orEmpty(), plan)
                        }
                    }.onSuccess { uri ->
                        ShareCardGenerator.shareImage(context, uri, "데이트 카드 공유")
                    }.onFailure {
                        shareError = it.message ?: "공유 카드를 만들지 못했어요"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DeleteRecordButton(
            label = "플랜 삭제",
            title = "데이트 플랜 삭제",
            message = "삭제할까요?",
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
            onConfirm = { viewModel.deleteDatePlan(plan.planId) },
        )
        shareError?.let { Text(it, color = RauschPink) }
    }
}

private fun DateMode.displayLabel(): String = when (this) {
    DateMode.Nearby -> "근처"
    DateMode.LongDistance -> "장거리"
    DateMode.Home -> "집"
}

private fun String.toneLabel(): String = when (this) {
    "safe" -> "편안한 플랜"
    "playful" -> "재밌는 플랜"
    "bold" -> "새 도전 플랜"
    else -> this
}

private fun String.vibeLabel(): String = when (this) {
    "Cozy" -> "포근"
    "Adventure" -> "모험"
    "Cute" -> "귀여운"
    "Chill" -> "느긋"
    "Foodie" -> "맛있는"
    "Creative" -> "창작"
    "Movie Night" -> "영화"
    "Walk & Talk" -> "산책토크"
    "Long Distance Call" -> "원거리 통화"
    else -> this
}

@Composable
private fun ScheduleDatePlanButton(
    plan: DatePlan,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSchedule: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var dateText by remember(plan.planId, plan.scheduledAt) {
        mutableStateOf(plan.scheduledAt?.toDateText() ?: LocalDate.now().format(dateFormatter))
    }
    var dateError by remember(plan.planId) { mutableStateOf<String?>(null) }
    SecondaryPastelButton(
        text = if (plan.scheduledAt == null) "예정일 설정" else "예정일 변경 · ${plan.scheduledAt.toDateText()}",
        onClick = { showDialog = true },
        modifier = modifier,
        enabled = enabled,
    )
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("데이트 예정일") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it.take(10)
                            dateError = null
                        },
                        label = { Text("날짜 YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = dateError != null,
                        supportingText = { Text(dateError ?: LocalDate.now().format(dateFormatter)) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedDate = dateText.toStartedAtMillis()
                        if (parsedDate == null) {
                            dateError = "날짜 형식을 확인해주세요"
                        } else {
                            showDialog = false
                            onSchedule(parsedDate)
                        }
                    },
                ) {
                    Text("예정으로 저장", color = RauschPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun MemoriesTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var clearAfterSave by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { picked = it.take(4) }

    LaunchedEffect(uiState.message) {
        if (clearAfterSave && uiState.message == "추억을 저장했어요") {
            title = ""
            note = ""
            picked = emptyList()
            clearAfterSave = false
        }
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) clearAfterSave = false
    }

    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Memory Scrapbook")
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth())
                SecondaryPastelButton(
                    "사진 선택 ${picked.size}/4",
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy,
                )
                RoundedPastelButton(
                    "추억 저장",
                    onClick = {
                        clearAfterSave = true
                        viewModel.saveMemory(title, note, picked)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && title.isNotBlank(),
                )
            }
        }
        if (uiState.memories.isEmpty()) item { EmptyState("추억 없음", "사진과 메모") }
        items(uiState.memories, key = { it.memoryId }) { memory ->
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var shareError by remember(memory.memoryId) { mutableStateOf<String?>(null) }
            val remainingImageSlots = (4 - memory.imageUrls.size).coerceAtLeast(0)
            val addImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
                if (uris.isNotEmpty()) viewModel.addMemoryImages(memory, uris)
            }
            SoftCard(Modifier.fillMaxWidth()) {
                Text(memory.title, style = MaterialTheme.typography.titleMedium)
                memory.note?.let { Text(it, color = WarmGray) }
                Text("사진 ${memory.imageUrls.size}/4장", color = RauschPink)
                if (memory.imageUrls.isNotEmpty()) {
                    MemoryImageStrip(
                        memory = memory,
                        enabled = !uiState.isBusy,
                        onRemove = { imageIndex -> viewModel.removeMemoryImage(memory, imageIndex) },
                    )
                } else {
                    Text("사진 없음", color = WarmGray, style = MaterialTheme.typography.bodySmall)
                }
                SecondaryPastelButton(
                    text = if (remainingImageSlots > 0) "사진 추가 ${memory.imageUrls.size}/4" else "사진 가득 참",
                    onClick = { addImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && remainingImageSlots > 0,
                )
                EditMemoryButton(
                    memory = memory,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onSave = { editedTitle, editedNote, editedDate ->
                        viewModel.updateMemory(memory.memoryId, editedTitle, editedNote, editedDate)
                    },
                )
                SecondaryPastelButton(
                    "공유 카드",
                    onClick = {
                        shareError = null
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    ShareCardGenerator.createMemoryCardUri(context, uiState.room?.title.orEmpty(), memory)
                                }
                            }.onSuccess { uri ->
                                ShareCardGenerator.shareImage(context, uri, "추억 카드 공유")
                            }.onFailure {
                                shareError = it.message ?: "공유 카드를 만들지 못했어요"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryPastelButton(
                    text = if (memory.imageUrls.isEmpty()) "콜라주 없음" else "콜라주",
                    onClick = {
                        shareError = null
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    ShareCardGenerator.createMemoryCollageUri(context, memory)
                                }
                            }.onSuccess { uri ->
                                ShareCardGenerator.shareImage(context, uri, "추억 콜라주 공유")
                            }.onFailure {
                                shareError = it.message ?: "콜라주를 만들지 못했어요"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && memory.imageUrls.isNotEmpty(),
                )
                DeleteRecordButton(
                    label = "추억 삭제",
                    title = "추억 삭제",
                    message = "삭제할까요?",
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onConfirm = { viewModel.deleteMemory(memory) },
                )
                shareError?.let { Text(it, color = RauschPink) }
            }
        }
    }
}

@Composable
private fun MemoryImageStrip(
    memory: MemoryItem,
    enabled: Boolean,
    onRemove: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        memory.imageUrls.forEachIndexed { index, url ->
            Column(
                modifier = Modifier.width(132.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(WarmCanvas),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "추억 사진 ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                DeleteRecordButton(
                    label = "사진 삭제",
                    title = "사진 삭제",
                    message = "이 사진만 추억에서 삭제할까요?",
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    onConfirm = { onRemove(index) },
                )
            }
        }
    }
}

@Composable
private fun QuizzesTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val yourTurn = uiState.quizQuestions.filter { it.myAnswer(uiState) == null }
    val discussionReady = uiState.quizQuestions.filter { it.isDiscussionReady(uiState) }
    val answered = uiState.quizQuestions.filter { it.myAnswer(uiState) != null && it !in discussionReady }
    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Couples Quizzes")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipLike("Your turn ${yourTurn.size}", selectedTab == 0, { selectedTab = 0 })
                    FilterChipLike("Answered ${answered.size}", selectedTab == 1, { selectedTab = 1 })
                    FilterChipLike("Discussion ${discussionReady.size}", selectedTab == 2, { selectedTab = 2 })
                }
            }
        }
        when (selectedTab) {
            0 -> {
                if (yourTurn.isEmpty()) item { EmptyState("내 차례 없음", "대기 중") }
                items(yourTurn, key = { "turn-${it.questionId}" }) { question ->
                    QuizAnswerCard(question = question, uiState = uiState, viewModel = viewModel)
                }
            }
            1 -> {
                if (answered.isEmpty()) item { EmptyState("대기 없음", "비어 있음") }
                items(answered, key = { "answered-${it.questionId}" }) { question ->
                    QuizAnsweredCard(question = question, uiState = uiState)
                }
            }
            else -> {
                if (discussionReady.isEmpty()) item { EmptyState("Discussion 없음", "비어 있음") }
                items(discussionReady, key = { "discussion-${it.questionId}" }) { question ->
                    QuizDiscussionCard(question = question, uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun QuizAnswerCard(question: QuizQuestion, uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var answer by remember(question.questionId) { mutableStateOf("") }
    var pickedImage by remember(question.questionId) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pickedImage = uri }
    val answers = question.answers(uiState)
    SoftCard(Modifier.fillMaxWidth()) {
        Text(question.category, color = RauschPink, style = MaterialTheme.typography.labelLarge)
        Text(question.question, style = MaterialTheme.typography.titleMedium)
        question.options?.let { options ->
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option -> FilterChipLike(option, answer == option, { answer = option }) }
            }
        }
        OutlinedTextField(value = answer, onValueChange = { answer = it.take(160) }, label = { Text("내 답변") }, modifier = Modifier.fillMaxWidth())
        SecondaryPastelButton(
            text = if (pickedImage == null) "답변 이미지 선택" else "답변 이미지 선택됨",
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBusy,
        )
        RoundedPastelButton(
            "답변 저장",
            onClick = {
                viewModel.answerQuiz(question.questionId, answer, pickedImage)
                answer = ""
                pickedImage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBusy && answer.isNotBlank(),
        )
        Text("답변 ${answers.size}/2", color = WarmGray)
    }
}

@Composable
private fun QuizAnsweredCard(question: QuizQuestion, uiState: RoomFeatureUiState) {
    val myAnswer = question.myAnswer(uiState)
    val answers = question.answers(uiState)
    SoftCard(Modifier.fillMaxWidth()) {
        Text(question.category, color = RauschPink, style = MaterialTheme.typography.labelLarge)
        Text(question.question, style = MaterialTheme.typography.titleMedium)
        Text("내 답변", style = MaterialTheme.typography.labelLarge)
        Text(myAnswer?.answer.orEmpty(), color = WarmGray)
        myAnswer?.imageUrl?.let { url -> QuizImage(url) }
        Text("대기 · ${answers.size}/2", color = RauschPink)
    }
}

@Composable
private fun QuizDiscussionCard(question: QuizQuestion, uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var message by remember(question.questionId) { mutableStateOf("") }
    var pickedImage by remember(question.questionId) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pickedImage = uri }
    val answers = question.answers(uiState).sortedBy { it.createdAt }
    val discussions = question.discussions(uiState)
    SoftCard(Modifier.fillMaxWidth()) {
        Text(question.category, color = RauschPink, style = MaterialTheme.typography.labelLarge)
        Text(question.question, style = MaterialTheme.typography.titleMedium)
        answers.forEach { answer ->
            Text(if (answer.uid == uiState.localUid) "내 답변" else "상대방 답변", style = MaterialTheme.typography.labelLarge)
            Text(answer.answer, color = WarmGray)
            answer.imageUrl?.let { url -> QuizImage(url) }
        }
        if (discussions.isEmpty()) {
            Text("대화 없음", color = WarmGray)
        } else {
            discussions.forEach { discussion ->
                Text(if (discussion.authorUid == uiState.localUid) "나" else "상대방", color = RauschPink, style = MaterialTheme.typography.labelLarge)
                if (discussion.message.isNotBlank()) Text(discussion.message, color = WarmGray)
                discussion.imageUrl?.let { url -> QuizImage(url) }
            }
        }
        OutlinedTextField(value = message, onValueChange = { message = it.take(180) }, label = { Text("메시지") }, modifier = Modifier.fillMaxWidth())
        SecondaryPastelButton(
            text = if (pickedImage == null) "이미지 1장 선택" else "이미지 선택됨",
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBusy,
        )
        RoundedPastelButton(
            "남기기",
            onClick = {
                viewModel.addQuizDiscussion(question.questionId, message, pickedImage)
                message = ""
                pickedImage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBusy && (message.isNotBlank() || pickedImage != null),
        )
    }
}

@Composable
private fun QuizImage(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(160.dp),
    )
}

private fun QuizQuestion.answers(uiState: RoomFeatureUiState): List<QuizAnswer> =
    uiState.quizAnswers.filter { it.questionId == questionId }

private fun QuizQuestion.discussions(uiState: RoomFeatureUiState): List<QuizDiscussion> =
    uiState.quizDiscussions.filter { it.questionId == questionId }

private fun QuizQuestion.myAnswer(uiState: RoomFeatureUiState): QuizAnswer? =
    answers(uiState).firstOrNull { it.uid == uiState.localUid }

private fun QuizQuestion.isDiscussionReady(uiState: RoomFeatureUiState): Boolean {
    val memberIds = uiState.room?.members?.filterValues { it }?.keys.orEmpty()
    if (memberIds.size < 2) return false
    val answeredIds = answers(uiState).map { it.uid }.toSet()
    return memberIds.all { it in answeredIds }
}

@Composable
private fun DailySparkTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    val spark = uiState.dailySpark
    val memberIds = uiState.room?.members?.filterValues { it }?.keys.orEmpty()
    val sparkAnswers = spark?.answers.orEmpty()
    val targetCount = memberIds.size.coerceAtLeast(2)
    val myAnswer = sparkAnswers[uiState.localUid]
    val isComplete = memberIds.size >= 2 && memberIds.all { uid -> sparkAnswers[uid]?.answer?.isNotBlank() == true }
    val discussionQuestionId = spark?.dateKey?.let(ModelFactory::dailySparkDiscussionId)
    val sparkDiscussions = uiState.quizDiscussions
        .filter { discussion -> discussion.questionId == discussionQuestionId }
        .sortedBy { it.createdAt }
    var answer by remember(spark?.dateKey, myAnswer?.answer) { mutableStateOf(myAnswer?.answer.orEmpty()) }
    var discussionMessage by remember(spark?.dateKey) { mutableStateOf("") }
    var pickedImage by remember(spark?.dateKey) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pickedImage = uri }
    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Daily Spark")
                Text(
                    if (uiState.stats.currentSparkStreak > 0) "스트릭 ${uiState.stats.currentSparkStreak}일" else "스트릭 0일",
                    color = RauschPink,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(spark?.question ?: "오늘의 질문을 준비 중이에요", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipLike("오늘 답변 ${sparkAnswers.size}/$targetCount", selected = isComplete, onClick = {})
                    FilterChipLike(if (myAnswer == null) "내 차례" else "내 답변 완료", selected = myAnswer != null, onClick = {})
                    FilterChipLike(if (isComplete) "둘 다 완료" else "상대방 대기", selected = isComplete, onClick = {})
                }
                if (myAnswer != null) {
                    Text("내 답변", style = MaterialTheme.typography.labelLarge)
                    Text(myAnswer.answer, color = WarmGray)
                }
                if (isComplete) {
                    sparkAnswers.values
                        .filter { it.uid != uiState.localUid }
                        .forEach { partnerAnswer ->
                            Text("상대방 답변", style = MaterialTheme.typography.labelLarge)
                            Text(partnerAnswer.answer, color = WarmGray)
                        }
                } else if (myAnswer != null) {
                    Text("상대방 대기", color = WarmGray)
                }
                OutlinedTextField(value = answer, onValueChange = { answer = it.take(160) }, label = { Text("오늘의 답변") }, modifier = Modifier.fillMaxWidth())
                RoundedPastelButton(
                    if (myAnswer == null) "답변하기" else "답변 업데이트",
                    onClick = { viewModel.answerDailySpark(answer) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && answer.isNotBlank(),
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Daily Spark Discussion")
                if (sparkDiscussions.isEmpty()) {
                    Text("대화 없음", color = WarmGray)
                } else {
                    sparkDiscussions.forEach { discussion ->
                        Text(if (discussion.authorUid == uiState.localUid) "나" else "상대방", color = RauschPink, style = MaterialTheme.typography.labelLarge)
                        if (discussion.message.isNotBlank()) Text(discussion.message, color = WarmGray)
                        discussion.imageUrl?.let { url -> QuizImage(url) }
                    }
                }
                OutlinedTextField(
                    value = discussionMessage,
                    onValueChange = { discussionMessage = it.take(180) },
                    label = { Text("메시지") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryPastelButton(
                    text = if (pickedImage == null) "이미지 1장 선택" else "이미지 선택됨",
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && spark != null,
                )
                RoundedPastelButton(
                    "남기기",
                    onClick = {
                        viewModel.addDailySparkDiscussion(discussionMessage, pickedImage)
                        discussionMessage = ""
                        pickedImage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && spark != null && (discussionMessage.isNotBlank() || pickedImage != null),
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Streak 상태")
                Text(
                    if (isComplete) "완료" else "진행 중",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun BucketTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var vibe by remember { mutableStateOf("Cozy") }
    val wishCount = uiState.bucketItems.count { it.status == BucketStatus.Wish.value }
    val plannedCount = uiState.bucketItems.count { it.status == BucketStatus.Planned.value }
    val doneCount = uiState.bucketItems.count { it.status == BucketStatus.Done.value }
    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Shared Bucket List")
                OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("해보고 싶은 것") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it.take(160) }, label = { Text("짧은 설명") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateIdeaGenerator.vibes.forEach { option ->
                        FilterChipLike(option.vibeLabel(), vibe == option, onClick = { vibe = option })
                    }
                }
                RoundedPastelButton(
                    "버킷에 추가",
                    onClick = {
                        viewModel.addBucketItem(title, description, vibe)
                        title = ""
                        description = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && title.isNotBlank(),
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("버킷 상태")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipLike("Wish $wishCount", selected = wishCount > 0, onClick = {}, modifier = Modifier.weight(1f))
                    FilterChipLike("Planned $plannedCount", selected = plannedCount > 0, onClick = {}, modifier = Modifier.weight(1f))
                    FilterChipLike("Done $doneCount", selected = doneCount > 0, onClick = {}, modifier = Modifier.weight(1f))
                }
            }
        }
        if (uiState.bucketItems.isEmpty()) {
            item { EmptyState("버킷 없음", "비어 있음") }
        }
        items(uiState.bucketItems, key = { it.itemId }) { item ->
            SoftCard(Modifier.fillMaxWidth()) {
                Text(item.bucketStatusLabel(), color = RauschPink, style = MaterialTheme.typography.labelLarge)
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                item.description?.let { Text(it, color = WarmGray) }
                item.vibe?.let { Text(it.vibeLabel(), color = RauschPink, style = MaterialTheme.typography.labelLarge) }
                item.plannedDatePlanId?.let {
                    Text("플랜 있음", color = WarmGray, style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BucketStatus.entries.forEach { status ->
                        FilterChipLike(status.displayLabel(), item.status == status.value, { viewModel.updateBucketStatus(item.itemId, status.value) })
                    }
                }
                SecondaryPastelButton(
                    text = if (item.plannedDatePlanId == null) "플랜 만들기" else "연결 완료",
                    onClick = { viewModel.createDatePlanFromBucket(item) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && item.plannedDatePlanId == null,
                )
                EditBucketButton(
                    item = item,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onSave = { editedTitle, editedDescription, editedVibe ->
                        viewModel.updateBucketItem(item.itemId, editedTitle, editedDescription, editedVibe)
                    },
                )
                DeleteRecordButton(
                    label = "버킷 삭제",
                    title = "버킷 항목 삭제",
                    message = "삭제할까요?",
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onConfirm = { viewModel.deleteBucketItem(item.itemId) },
                )
            }
        }
    }
}

private fun BucketItem.bucketStatusLabel(): String =
    BucketStatus.entries.firstOrNull { it.value == status }?.displayLabel() ?: status

private fun BucketStatus.displayLabel(): String = when (this) {
    BucketStatus.Wish -> "Wish"
    BucketStatus.Planned -> "Planned"
    BucketStatus.Done -> "Done"
}

@Composable
private fun EditNoteButton(
    note: LoveNote,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSave: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var message by remember(note.noteId, note.updatedAt, note.message) { mutableStateOf(note.message) }
    SecondaryPastelButton("편집", onClick = { showDialog = true }, modifier = modifier, enabled = enabled)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("노트 편집") },
            text = {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(120) },
                    label = { Text("짧은 메시지") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = message.isNotBlank(),
                    onClick = {
                        showDialog = false
                        onSave(message)
                    },
                ) {
                    Text("저장", color = RauschPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun EditDatePlanButton(
    plan: DatePlan,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSave: (String, String, String, String, String, String, Long?) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember(plan.planId, plan.updatedAt, plan.title) { mutableStateOf(plan.title) }
    var description by remember(plan.planId, plan.updatedAt, plan.description) { mutableStateOf(plan.description) }
    var vibe by remember(plan.planId, plan.updatedAt, plan.vibe) { mutableStateOf(plan.vibe) }
    var budget by remember(plan.planId, plan.updatedAt, plan.estimatedBudget) { mutableStateOf(plan.estimatedBudget.orEmpty()) }
    var time by remember(plan.planId, plan.updatedAt, plan.estimatedTime) { mutableStateOf(plan.estimatedTime.orEmpty()) }
    var stepsText by remember(plan.planId, plan.updatedAt, plan.steps) { mutableStateOf(plan.steps.joinToString("\n")) }
    var scheduledAtText by remember(plan.planId, plan.updatedAt, plan.scheduledAt) { mutableStateOf(plan.scheduledAt.toDateText()) }
    var dateError by remember(plan.planId) { mutableStateOf<String?>(null) }
    SecondaryPastelButton("플랜 편집", onClick = { showDialog = true }, modifier = modifier, enabled = enabled)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("데이트 플랜 편집") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it.take(220) }, label = { Text("설명") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = vibe, onValueChange = { vibe = it.take(40) }, label = { Text("분위기") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = budget, onValueChange = { budget = it.take(24) }, label = { Text("예산") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = time, onValueChange = { time = it.take(24) }, label = { Text("시간") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = scheduledAtText,
                        onValueChange = {
                            scheduledAtText = it.take(10)
                            dateError = null
                        },
                        label = { Text("예정일 YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = dateError != null,
                        supportingText = dateError?.let { { Text(it) } },
                    )
                    OutlinedTextField(value = stepsText, onValueChange = { stepsText = it.take(320) }, label = { Text("스텝, 줄마다 1개") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && description.isNotBlank(),
                    onClick = {
                        val parsedDate = scheduledAtText.toStartedAtMillis()
                        if (scheduledAtText.isNotBlank() && parsedDate == null) {
                            dateError = "날짜 형식을 확인해주세요"
                        } else {
                            showDialog = false
                            onSave(title, description, vibe, budget, time, stepsText, parsedDate)
                        }
                    },
                ) {
                    Text("저장", color = RauschPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun EditMemoryButton(
    memory: MemoryItem,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSave: (String, String?, Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember(memory.memoryId, memory.updatedAt, memory.title) { mutableStateOf(memory.title) }
    var note by remember(memory.memoryId, memory.updatedAt, memory.note) { mutableStateOf(memory.note.orEmpty()) }
    var dateText by remember(memory.memoryId, memory.updatedAt, memory.date) { mutableStateOf(memory.date.toDateText()) }
    var dateError by remember(memory.memoryId) { mutableStateOf<String?>(null) }
    SecondaryPastelButton("추억 편집", onClick = { showDialog = true }, modifier = modifier, enabled = enabled)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("추억 편집") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it.take(220) }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it.take(10)
                            dateError = null
                        },
                        label = { Text("날짜 YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = dateError != null,
                        supportingText = dateError?.let { { Text(it) } },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        val parsedDate = dateText.toStartedAtMillis()
                        if (parsedDate == null) {
                            dateError = "날짜 형식을 확인해주세요"
                        } else {
                            showDialog = false
                            onSave(title, note, parsedDate)
                        }
                    },
                ) {
                    Text("저장", color = RauschPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun EditBucketButton(
    item: BucketItem,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSave: (String, String?, String?) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember(item.itemId, item.updatedAt, item.title) { mutableStateOf(item.title) }
    var description by remember(item.itemId, item.updatedAt, item.description) { mutableStateOf(item.description.orEmpty()) }
    var vibe by remember(item.itemId, item.updatedAt, item.vibe) { mutableStateOf(item.vibe.orEmpty()) }
    SecondaryPastelButton("버킷 편집", onClick = { showDialog = true }, modifier = modifier, enabled = enabled)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("버킷 항목 편집") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it.take(160) }, label = { Text("설명") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = vibe, onValueChange = { vibe = it.take(40) }, label = { Text("분위기") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        showDialog = false
                        onSave(title, description, vibe)
                    },
                ) {
                    Text("저장", color = RauschPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun DeleteRecordButton(
    label: String,
    title: String,
    message: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SecondaryPastelButton(label, onClick = { showDialog = true }, modifier = modifier, enabled = enabled)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(message, color = WarmGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onConfirm()
                    },
                ) {
                    Text("삭제", color = RauschPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun StatsTab(uiState: RoomFeatureUiState, viewModel: RoomFeatureViewModel) {
    val context = LocalContext.current
    var title by remember(uiState.room?.title) { mutableStateOf(uiState.room?.title.orEmpty()) }
    var startedAtText by remember(uiState.room?.startedAt) { mutableStateOf(uiState.room?.startedAt.toDateText()) }
    var privacyMode by remember(uiState.room?.privacyMode) { mutableStateOf(uiState.room?.privacyMode ?: false) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val localLocationShare = uiState.locationShares.firstOrNull { it.uid == uiState.localUid }
    val partnerLocationShare = uiState.locationShares.firstOrNull { it.uid != uiState.localUid }
    val legalLinks = remember { ReleaseLegalConfig.current() }
    val localLocationConsent = localLocationShare?.enabled == true
    val partnerLocationConsent = partnerLocationShare?.enabled == true
    val canShareLocation = localLocationConsent && partnerLocationConsent
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (!canShareLocation) {
            locationError = "동의 필요"
        } else if (grants.values.any { it }) {
            shareOneShotLocation(
                context = context,
                approximateOnly = grants[Manifest.permission.ACCESS_FINE_LOCATION] != true,
                onError = { locationError = it },
                onShare = viewModel::shareCurrentLocation,
            )
        } else {
            locationError = "권한 필요"
        }
    }
    TabScaffold {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Streaks & Stats")
                Text("D+${uiState.stats.daysTogether}", style = MaterialTheme.typography.titleLarge)
                Text("데이트 ${uiState.stats.monthlyDateCount} · 예정 ${uiState.stats.upcomingDateCount}")
                Text("Spark ${uiState.stats.currentSparkStreak}일")
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("방 설정")
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("방 이름") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = startedAtText,
                    onValueChange = {
                        startedAtText = it.take(10)
                        dateError = null
                    },
                    label = { Text("사귄 시작일 YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = dateError != null,
                    supportingText = { Text(dateError ?: "2026-05-27") },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Privacy Mode", style = MaterialTheme.typography.titleMedium)
                        Text("위젯 숨김", color = WarmGray)
                    }
                    Switch(checked = privacyMode, onCheckedChange = { privacyMode = it })
                }
                RoundedPastelButton(
                    "저장",
                    onClick = {
                        val startedAt = startedAtText.toStartedAtMillis()
                        if (startedAtText.isNotBlank() && startedAt == null) {
                            dateError = "날짜 형식을 확인해주세요"
                        } else {
                            viewModel.updateRoomSettings(title, startedAt, privacyMode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            val isWidgetRoom = uiState.widgetRoomId == uiState.room?.roomId
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("위젯 대표방")
                RoundedPastelButton(
                    if (isWidgetRoom) "대표방 저장" else "대표방 설정",
                    onClick = viewModel::setWidgetRoom,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy,
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("Distance Widget")
                Text(uiState.distanceText, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("내 위치 공유 동의", style = MaterialTheme.typography.titleMedium)
                        Text(if (localLocationConsent) "마지막 공유 ${localLocationShare.lastSharedAt.toDateTimeText()}" else "공유 안 함", color = WarmGray)
                    }
                    Switch(
                        checked = localLocationConsent,
                        onCheckedChange = {
                            locationError = null
                            viewModel.setLocationSharingEnabled(it)
                        },
                    )
                }
                Text(
                    if (partnerLocationConsent) "상대방 동의 완료" else "상대방 동의 필요",
                    color = WarmGray,
                )
                RoundedPastelButton(
                    when {
                        !localLocationConsent -> "위치 공유 동의 켜기"
                        !partnerLocationConsent -> "상대방 동의 대기 중"
                        else -> "현재 위치 1회 공유"
                    },
                    onClick = {
                        locationError = null
                        if (!localLocationConsent) {
                            viewModel.setLocationSharingEnabled(true)
                        } else if (!partnerLocationConsent) {
                            locationError = "상대방 동의 필요"
                        } else if (context.hasLocationPermission()) {
                            shareOneShotLocation(
                                context = context,
                                approximateOnly = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED,
                                onError = { locationError = it },
                                onShare = viewModel::shareCurrentLocation,
                            )
                        } else {
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy && (localLocationConsent || uiState.room?.members?.keys.orEmpty().size >= 2),
                )
                if (localLocationConsent && !partnerLocationConsent) {
                    Text("상대방 동의 필요", color = WarmGray)
                }
                locationError?.let { Text(it, color = RauschPink) }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("개인정보와 권한")
                if (!uiState.isFirebaseConnected) {
                    Text("재연결 중", color = RauschPink)
                }
                Text("화면 위 그리기: 사용자가 켠 동안만 낙서 오버레이를 보여줘요.", color = WarmGray)
                Text("화면 캡처 없음: 다른 앱 화면이나 입력 내용을 읽지 않아요.", color = WarmGray)
                Text("알림: 그리기 시작, 끄기, 전체 지우기 조작에만 사용해요.", color = WarmGray)
                Text("사진: 사용자가 직접 선택한 이미지만 저장해요.", color = WarmGray)
                Text("위치: 양쪽 동의 후 버튼을 눌렀을 때만 1회 공유해요.", color = WarmGray)
                Text("초대: QR과 링크는 사용자가 직접 보낸 상대에게만 전달돼요.", color = WarmGray)
                Text("광고/분석 SDK 없음", color = WarmGray)
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                SectionTitle("개인정보 처리")
                Text(
                    if (legalLinks.isReleaseReady) {
                        "개인정보처리방침, 계정/데이터 삭제 요청, 문의 경로가 준비되어 있어요."
                    } else {
                        "출시 전 운영자 연락처, 개인정보처리방침 URL, 계정/데이터 삭제 URL을 설정해야 해요."
                    },
                    color = WarmGray,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryPastelButton(
                        "처리방침",
                        onClick = { context.openWebUrl(legalLinks.privacyPolicyUrl) },
                        modifier = Modifier.weight(1f),
                        enabled = legalLinks.hasPrivacyPolicyUrl,
                    )
                    SecondaryPastelButton(
                        "삭제 요청",
                        onClick = { context.openWebUrl(legalLinks.accountDeletionUrl) },
                        modifier = Modifier.weight(1f),
                        enabled = legalLinks.hasAccountDeletionUrl,
                    )
                }
                SecondaryPastelButton(
                    "문의 메일 보내기",
                    onClick = { context.sendSupportEmail(legalLinks.supportEmail) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = legalLinks.hasSupportEmail,
                )
            }
        }
    }
}

private fun Long?.toDateText(): String =
    this?.takeIf { it > 0L }?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }.orEmpty()

private fun Long?.toDatePlanLabel(): String =
    this?.takeIf { it > 0L }?.let {
        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        val dDay = when {
            days == 0 -> "D-Day"
            days > 0 -> "D-$days"
            else -> "지난 일정"
        }
        "$dDay · ${date.format(dateFormatter)}"
    } ?: "예정일 미정"

private fun String.toStartedAtMillis(): Long? =
    trim().ifBlank { return null }.let {
        runCatching {
            LocalDate.parse(it, dateFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }

private fun Long?.toDateTimeText(): String =
    this?.takeIf { it > 0L }?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    } ?: "없음"

private fun Context.hasLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

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

@SuppressLint("MissingPermission")
private fun shareOneShotLocation(
    context: Context,
    approximateOnly: Boolean,
    onError: (String) -> Unit,
    onShare: (Double, Double, Float?, Boolean) -> Unit,
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = latestKnownLocation(locationManager)
    if (location == null) {
        onError("위치 확인 실패")
    } else {
        onShare(location.latitude, location.longitude, location.accuracy.takeIf { it > 0f }, approximateOnly)
    }
}

@SuppressLint("MissingPermission")
private fun latestKnownLocation(locationManager: LocationManager): Location? {
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return providers
        .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}
