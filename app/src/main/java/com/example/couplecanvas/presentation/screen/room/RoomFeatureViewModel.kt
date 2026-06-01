package com.example.couplecanvas.presentation.screen.room

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couplecanvas.data.local.WidgetStateStore
import com.example.couplecanvas.data.model.BucketItem
import com.example.couplecanvas.data.model.CoupleStats
import com.example.couplecanvas.data.model.DailySpark
import com.example.couplecanvas.data.model.DateMode
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DateVote
import com.example.couplecanvas.data.model.LoveNote
import com.example.couplecanvas.data.model.LocationShareState
import com.example.couplecanvas.data.model.MemoryItem
import com.example.couplecanvas.data.model.ModelFactory
import com.example.couplecanvas.data.model.QuizAnswer
import com.example.couplecanvas.data.model.QuizDiscussion
import com.example.couplecanvas.data.model.QuizQuestion
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.WidgetSnapshot
import com.example.couplecanvas.data.repository.AuthRepository
import com.example.couplecanvas.data.repository.FeatureRepository
import com.example.couplecanvas.data.repository.RoomRepository
import com.example.couplecanvas.feature.widgets.updateCoupleWidgets
import com.example.couplecanvas.util.DateIdeaGenerator
import com.example.couplecanvas.util.DistanceCalculator
import com.example.couplecanvas.util.QuizBank
import com.example.couplecanvas.util.StatsCalculator
import com.example.couplecanvas.util.WidgetSnapshotFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class RoomFeatureUiState(
    val localUid: String = "",
    val widgetRoomId: String = "",
    val room: Room? = null,
    val notes: List<LoveNote> = emptyList(),
    val datePlans: List<DatePlan> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val bucketItems: List<BucketItem> = emptyList(),
    val quizQuestions: List<QuizQuestion> = QuizBank.defaultQuestions(),
    val quizAnswers: List<QuizAnswer> = emptyList(),
    val quizDiscussions: List<QuizDiscussion> = emptyList(),
    val dailySpark: DailySpark? = null,
    val dailySparks: List<DailySpark> = emptyList(),
    val locationShares: List<LocationShareState> = emptyList(),
    val distanceText: String = "위치 공유가 꺼져 있어요",
    val stats: CoupleStats = CoupleStats(),
    val isBusy: Boolean = false,
    val isFirebaseConnected: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class RoomFeatureViewModel(
    private val roomId: String,
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val featureRepository: FeatureRepository,
    private val widgetStateStore: WidgetStateStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RoomFeatureUiState(localUid = authRepository.currentUser?.uid.orEmpty()))
    val uiState: StateFlow<RoomFeatureUiState> = _uiState
    private val dateKey = ModelFactory.dateKey()
    private var widgetSnapshotJob: Job? = null

    init {
        viewModelScope.launch {
            widgetStateStore.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(widgetRoomId = snapshot.roomId)
            }
        }
        viewModelScope.launch {
            roomRepository.observeFirebaseConnection().collect { connected ->
                _uiState.value = _uiState.value.copy(isFirebaseConnected = connected)
            }
        }
        viewModelScope.launch {
            featureRepository.ensureDailySpark(roomId, dateKey)
            featureRepository.observeDailySpark(roomId, dateKey).collect { spark ->
                _uiState.value = _uiState.value.copy(dailySpark = spark)
                updateStatsAndWidget()
            }
        }
        viewModelScope.launch {
            featureRepository.observeDailySparks(roomId).collect { sparks ->
                _uiState.value = _uiState.value.copy(dailySparks = sparks.sortedByDescending { it.dateKey })
                updateStatsAndWidget()
            }
        }
        viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { room ->
                _uiState.value = _uiState.value.copy(room = room)
                updateStatsAndWidget()
            }
        }
        viewModelScope.launch {
            featureRepository.observeNotes(roomId).collect { notes ->
                _uiState.value = _uiState.value.copy(notes = notes.sortedWith(compareByDescending<LoveNote> { it.isPinned }.thenByDescending { maxOf(it.updatedAt, it.createdAt) }))
                updateStatsAndWidget()
            }
        }
        viewModelScope.launch {
            featureRepository.observeDatePlans(roomId).collect { plans ->
                _uiState.value = _uiState.value.copy(datePlans = plans.sortedByDescending { maxOf(it.updatedAt, it.createdAt) })
                updateStatsAndWidget()
            }
        }
        viewModelScope.launch {
            featureRepository.observeMemories(roomId).collect { memories ->
                _uiState.value = _uiState.value.copy(memories = memories.sortedWith(compareByDescending<MemoryItem> { it.date }.thenByDescending { maxOf(it.updatedAt, it.createdAt) }))
                updateStatsAndWidget()
            }
        }
        viewModelScope.launch {
            featureRepository.observeBucketItems(roomId).collect { items ->
                _uiState.value = _uiState.value.copy(bucketItems = items.sortedByDescending { it.updatedAt })
            }
        }
        viewModelScope.launch {
            featureRepository.observeQuizAnswers(roomId).collect { answers ->
                _uiState.value = _uiState.value.copy(quizAnswers = answers.sortedByDescending { it.createdAt })
            }
        }
        viewModelScope.launch {
            featureRepository.observeQuizDiscussions(roomId).collect { discussions ->
                _uiState.value = _uiState.value.copy(quizDiscussions = discussions.sortedBy { it.createdAt })
            }
        }
        viewModelScope.launch {
            featureRepository.observeLocationShares(roomId).collect { shares ->
                _uiState.value = _uiState.value.copy(
                    locationShares = shares,
                    distanceText = DistanceCalculator.distanceText(_uiState.value.localUid, shares),
                )
                updateStatsAndWidget()
            }
        }
    }

    fun sendNote(message: String) {
        val uid = authRepository.currentUser?.uid ?: return
        if (message.isBlank()) return
        viewModelScope.launchBusy(successMessage = "노트를 보냈어요") { featureRepository.sendNote(roomId, uid, message) }
    }

    fun toggleNotePin(note: LoveNote) {
        viewModelScope.launchBusy { featureRepository.togglePin(roomId, note) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launchBusy(successMessage = "노트를 삭제했어요") { featureRepository.deleteNote(roomId, noteId) }
    }

    fun updateNote(noteId: String, message: String) {
        if (message.isBlank()) return
        viewModelScope.launchBusy(successMessage = "노트를 수정했어요") { featureRepository.updateNote(roomId, noteId, message) }
    }

    fun markReceivedNotesRead() {
        val uid = authRepository.currentUser?.uid ?: return
        val hasUnreadReceivedNotes = _uiState.value.notes.any { it.authorUid != uid && !it.isRead }
        if (!hasUnreadReceivedNotes) return
        viewModelScope.launch {
            runCatching { featureRepository.markReceivedNotesRead(roomId, uid) }
        }
    }

    fun generateDateIdeas(mode: DateMode, vibe: String, budget: String, time: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launchBusy {
            DateIdeaGenerator.generate(uid, mode, vibe, budget, time).forEach { featureRepository.saveDatePlan(roomId, it) }
        }
    }

    fun addCustomDatePlan(
        title: String,
        description: String,
        mode: DateMode,
        vibe: String,
        budget: String,
        time: String,
        stepsText: String,
        scheduledAt: Long?,
    ) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launchBusy(successMessage = "데이트 플랜을 저장했어요") {
            val plan = DateIdeaGenerator.customPlan(
                uid = uid,
                title = title,
                description = description,
                mode = mode,
                vibe = vibe,
                budget = budget,
                time = time,
                stepsText = stepsText,
                scheduledAt = scheduledAt,
            )
            featureRepository.saveDatePlan(roomId, plan)
        }
    }

    fun votePlan(planId: String, vote: String) {
        val uid = authRepository.currentUser?.uid ?: return
        val message = when (DateVote.from(vote)) {
            DateVote.Like -> "좋아요"
            DateVote.Nope -> "별로"
            DateVote.Later -> "나중에"
            null -> "저장됨"
        }
        viewModelScope.launchBusy(successMessage = message) { featureRepository.voteDatePlan(roomId, planId, uid, vote) }
    }

    fun updateDateStatus(planId: String, status: DatePlanStatus) {
        viewModelScope.launchBusy { featureRepository.updateDateStatus(roomId, planId, status) }
    }

    fun scheduleDatePlan(planId: String, scheduledAt: Long) {
        viewModelScope.launchBusy(successMessage = "예정일을 설정했어요") {
            featureRepository.updateDateStatus(roomId, planId, DatePlanStatus.Upcoming, scheduledAt)
        }
    }

    fun deleteDatePlan(planId: String) {
        viewModelScope.launchBusy(successMessage = "데이트 플랜을 삭제했어요") { featureRepository.deleteDatePlan(roomId, planId) }
    }

    fun updateDatePlan(plan: DatePlan, title: String, description: String, vibe: String, budget: String, time: String, stepsText: String, scheduledAt: Long?) {
        if (title.isBlank() || description.isBlank()) return
        val updatedPlan = plan.copy(
            title = title,
            description = description,
            vibe = vibe.ifBlank { plan.vibe },
            estimatedBudget = budget,
            estimatedTime = time,
            steps = stepsText.lines().map { it.trim() }.filter { it.isNotEmpty() },
            scheduledAt = scheduledAt,
        )
        viewModelScope.launchBusy(successMessage = "데이트 플랜을 수정했어요") { featureRepository.updateDatePlan(roomId, updatedPlan) }
    }

    fun addBucketItem(title: String, vibe: String?) {
        val uid = authRepository.currentUser?.uid ?: return
        if (title.isBlank()) return
        val now = System.currentTimeMillis()
        val item = BucketItem(
            itemId = UUID.randomUUID().toString(),
            title = title.trim(),
            vibe = vibe?.trim()?.takeIf { it.isNotEmpty() },
            createdByUid = uid,
            createdAt = now,
            updatedAt = now,
        )
        viewModelScope.launchBusy(successMessage = "버킷리스트에 추가했어요") { featureRepository.saveBucketItem(roomId, item) }
    }

    fun addBucketItem(title: String, description: String?, vibe: String?) {
        val uid = authRepository.currentUser?.uid ?: return
        if (title.isBlank()) return
        val now = System.currentTimeMillis()
        val item = BucketItem(
            itemId = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            vibe = vibe?.trim()?.takeIf { it.isNotEmpty() },
            createdByUid = uid,
            createdAt = now,
            updatedAt = now,
        )
        viewModelScope.launchBusy(successMessage = "버킷리스트에 추가했어요") { featureRepository.saveBucketItem(roomId, item) }
    }

    fun updateBucketStatus(itemId: String, status: String) {
        viewModelScope.launchBusy { featureRepository.updateBucketStatus(roomId, itemId, status) }
    }

    fun deleteBucketItem(itemId: String) {
        viewModelScope.launchBusy(successMessage = "버킷 항목을 삭제했어요") { featureRepository.deleteBucketItem(roomId, itemId) }
    }

    fun updateBucketItem(itemId: String, title: String, description: String?, vibe: String?) {
        if (title.isBlank()) return
        viewModelScope.launchBusy(successMessage = "버킷 항목을 수정했어요") {
            featureRepository.updateBucketItem(roomId, itemId, title, description, vibe)
        }
    }

    fun createDatePlanFromBucket(item: BucketItem) {
        val uid = authRepository.currentUser?.uid ?: return
        if (item.plannedDatePlanId != null) {
            _uiState.value = _uiState.value.copy(message = "이미 플랜이 있어요")
            return
        }
        viewModelScope.launchBusy(successMessage = "버킷을 데이트 플랜으로 만들었어요") {
            val plan = DateIdeaGenerator.customPlan(
                uid = uid,
                title = item.title,
                description = item.description ?: "버킷 플랜",
                mode = DateMode.Nearby,
                vibe = item.vibe ?: "Cozy",
                budget = "보통",
                time = "1시간",
                stepsText = listOf(
                    "서로 가능한 날짜를 정해요",
                    item.title,
                    "끝나고 추억으로 남겨요",
                ).joinToString("\n"),
                scheduledAt = null,
            )
            featureRepository.planBucketItem(roomId, item, plan)
        }
    }

    fun saveMemory(title: String, note: String?, imageUris: List<Uri> = emptyList()) {
        val uid = authRepository.currentUser?.uid ?: return
        if (title.isBlank()) return
        viewModelScope.launchBusy(successMessage = "추억을 저장했어요") {
            val saved = featureRepository.saveMemory(roomId, uid, title, note, System.currentTimeMillis(), imageUris)
            val localPreviewPath = imageUris.firstOrNull()?.let { uri ->
                withContext(Dispatchers.IO) { widgetStateStore.saveLatestMemoryPreview(roomId, saved.memoryId, uri) }
            }
            if (widgetStateStore.shouldUpdateRoom(roomId)) {
                widgetStateStore.updateLatestMemory(
                    roomId = roomId,
                    title = saved.title,
                    memoryId = saved.memoryId,
                    latestMemoryImageUrl = saved.imageUrls.firstOrNull(),
                    latestMemoryLocalPath = localPreviewPath,
                    afterSave = ::updateCoupleWidgets,
                )
            }
        }
    }

    fun deleteMemory(memory: MemoryItem) {
        viewModelScope.launchBusy(successMessage = "추억을 삭제했어요") { featureRepository.deleteMemory(roomId, memory) }
    }

    fun updateMemory(memoryId: String, title: String, note: String?, date: Long) {
        if (title.isBlank()) return
        viewModelScope.launchBusy(successMessage = "추억을 수정했어요") {
            featureRepository.updateMemory(roomId, memoryId, title, note, date)
        }
    }

    fun addMemoryImages(memory: MemoryItem, imageUris: List<Uri>) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launchBusy(successMessage = "사진을 추가했어요") {
            val updated = featureRepository.addMemoryImages(roomId, uid, memory, imageUris)
            val isLatestMemory = _uiState.value.memories.firstOrNull()?.memoryId == memory.memoryId
            val shouldUsePickedAsCover = isLatestMemory && memory.imageUrls.isEmpty()
            val localPreviewPath = if (shouldUsePickedAsCover) {
                imageUris.firstOrNull()?.let { uri ->
                    withContext(Dispatchers.IO) { widgetStateStore.saveLatestMemoryPreview(roomId, updated.memoryId, uri) }
                }
            } else {
                null
            }
            if (isLatestMemory) {
                if (widgetStateStore.shouldUpdateRoom(roomId)) {
                    widgetStateStore.updateLatestMemory(
                        roomId = roomId,
                        title = updated.title,
                        memoryId = updated.memoryId,
                        latestMemoryImageUrl = updated.imageUrls.firstOrNull(),
                        latestMemoryLocalPath = localPreviewPath,
                        afterSave = ::updateCoupleWidgets,
                    )
                }
            }
        }
    }

    fun removeMemoryImage(memory: MemoryItem, imageIndex: Int) {
        viewModelScope.launchBusy(successMessage = "사진을 삭제했어요") {
            featureRepository.removeMemoryImage(roomId, memory, imageIndex)
        }
    }

    fun answerQuiz(questionId: String, answer: String, imageUri: Uri? = null) {
        val uid = authRepository.currentUser?.uid ?: return
        if (answer.isBlank()) return
        viewModelScope.launchBusy(successMessage = "답변을 저장했어요") { featureRepository.answerQuiz(roomId, uid, questionId, answer, imageUri) }
    }

    fun addQuizDiscussion(questionId: String, message: String, imageUri: Uri? = null) {
        val uid = authRepository.currentUser?.uid ?: return
        if (message.isBlank() && imageUri == null) return
        viewModelScope.launchBusy(successMessage = "토론에 남겼어요") { featureRepository.addQuizDiscussion(roomId, uid, questionId, message, imageUri) }
    }

    fun addDailySparkDiscussion(message: String, imageUri: Uri? = null) {
        val uid = authRepository.currentUser?.uid ?: return
        if (message.isBlank() && imageUri == null) return
        viewModelScope.launchBusy(successMessage = "Daily Spark 대화를 남겼어요") {
            featureRepository.addQuizDiscussion(roomId, uid, ModelFactory.dailySparkDiscussionId(dateKey), message, imageUri)
        }
    }

    fun answerDailySpark(answer: String) {
        val uid = authRepository.currentUser?.uid ?: return
        if (answer.isBlank()) return
        viewModelScope.launchBusy(successMessage = "Daily Spark 답변을 저장했어요") { featureRepository.answerDailySpark(roomId, dateKey, uid, answer) }
    }

    fun updateRoomSettings(title: String, startedAt: Long?, privacyMode: Boolean) {
        viewModelScope.launchBusy(successMessage = "방 설정을 저장했어요") {
            roomRepository.updateRoomSettings(roomId, title.ifBlank { "둘만의 그림방" }, startedAt, privacyMode)
        }
    }

    fun setWidgetRoom() {
        viewModelScope.launchBusy(successMessage = "이 방을 위젯 대표방으로 설정했어요") {
            val state = _uiState.value
            saveWidgetSnapshot(state, state.stats, state.distanceText, force = true)
        }
    }

    fun setLocationSharingEnabled(enabled: Boolean) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launchBusy(successMessage = if (enabled) "위치 공유 동의를 켰어요" else "위치 공유를 껐어요") {
            featureRepository.setLocationSharingEnabled(roomId, uid, enabled)
        }
    }

    fun shareCurrentLocation(latitude: Double, longitude: Double, accuracyMeters: Float?, approximateOnly: Boolean) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launchBusy(successMessage = "현재 위치를 1회 공유했어요") {
            featureRepository.shareCurrentLocation(roomId, uid, latitude, longitude, accuracyMeters, approximateOnly)
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }

    private suspend fun updateStatsAndWidget() {
        val state = _uiState.value
        val memberIds = state.room?.members?.filterValues { it }?.keys.orEmpty()
        val stats = CoupleStats(
            daysTogether = StatsCalculator.daysTogether(state.room?.startedAt),
            currentSparkStreak = StatsCalculator.currentSparkStreak(state.dailySparks, memberIds),
            monthlyDateCount = StatsCalculator.monthlyDateCount(state.datePlans),
            upcomingDateCount = StatsCalculator.upcomingDateCount(state.datePlans),
            lastSyncedAt = System.currentTimeMillis(),
            unreadNoteCount = state.notes.count { it.authorUid != state.localUid && !it.isRead },
        )
        val distanceText = DistanceCalculator.distanceText(state.localUid, state.locationShares)
        val updatedState = state.copy(stats = stats, distanceText = distanceText)
        _uiState.value = updatedState
        saveWidgetSnapshot(updatedState, stats, distanceText, force = false)
    }

    private suspend fun saveWidgetSnapshot(
        state: RoomFeatureUiState,
        stats: CoupleStats,
        distanceText: String,
        force: Boolean,
    ) {
        if (!force && !widgetStateStore.shouldUpdateRoom(roomId)) return
        if (!force) {
            widgetSnapshotJob?.cancel()
            widgetSnapshotJob = viewModelScope.launch {
                delay(WIDGET_SNAPSHOT_DEBOUNCE_MS)
                persistWidgetSnapshot(state, stats, distanceText)
            }
            return
        }
        widgetSnapshotJob?.cancel()
        persistWidgetSnapshot(state, stats, distanceText)
    }

    private suspend fun persistWidgetSnapshot(
        state: RoomFeatureUiState,
        stats: CoupleStats,
        distanceText: String,
    ) {
        val latestMemory = state.memories.firstOrNull()
        val memberIds = state.room?.members?.filterValues { it }?.keys.orEmpty()
        val roomTitle = state.room?.title.orEmpty()
        val latestMemoryImageUrl = latestMemory?.imageUrls?.firstOrNull()
        val latestMemoryLocalPath = if (state.room?.privacyMode == true || latestMemory == null) {
            null
        } else {
            runCatching {
                widgetStateStore.cacheLatestMemoryPreviewFromUrl(
                    roomId = roomId,
                    memoryId = latestMemory.memoryId,
                    imageUrl = latestMemoryImageUrl,
                )
            }.getOrNull()
        }
        widgetStateStore.save(
            WidgetSnapshot(
                roomId = roomId,
                roomTitle = roomTitle,
                privacyMode = state.room?.privacyMode ?: false,
                daysTogetherText = WidgetSnapshotFactory.daysTogetherText(stats.daysTogether),
                nextDateText = StatsCalculator.nextDateCountdownText(state.datePlans),
                latestNoteText = state.notes.firstOrNull()?.message ?: "노트 없음",
                dailySparkText = state.dailySpark.toWidgetSparkText(stats.currentSparkStreak, memberIds),
                latestMemoryTitle = latestMemory?.title ?: "추억 없음",
                latestMemoryId = latestMemory?.memoryId,
                latestMemoryImageUrl = latestMemoryImageUrl,
                latestMemoryLocalPath = latestMemoryLocalPath,
                statsText = "스트릭 ${stats.currentSparkStreak}일 · 데이트 ${stats.monthlyDateCount}회 · 예정 ${stats.upcomingDateCount}개",
                distanceText = distanceText,
                updatedAt = System.currentTimeMillis(),
            ),
            afterSave = ::updateCoupleWidgets,
        )
    }

    private fun DailySpark?.toWidgetSparkText(streak: Int, memberIds: Set<String>): String {
        val spark = this ?: return "질문 없음"
        val completed = StatsCalculator.isSparkComplete(spark, memberIds)
        return when {
            completed && streak > 0 -> "스트릭 ${streak}일"
            completed -> "완료"
            spark.answers.isNotEmpty() -> "대기 중"
            else -> spark.question
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.launchBusy(successMessage: String? = null, block: suspend () -> Unit) {
        launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null)
            runCatching { block() }
                .onSuccess {
                    if (successMessage != null) {
                        _uiState.value = _uiState.value.copy(message = successMessage)
                    }
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "작업에 실패했어요") }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    companion object {
        private const val WIDGET_SNAPSHOT_DEBOUNCE_MS = 400L
    }
}
