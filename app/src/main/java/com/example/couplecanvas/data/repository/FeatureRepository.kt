package com.example.couplecanvas.data.repository

import android.net.Uri
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.BucketItem
import com.example.couplecanvas.data.model.BucketStatus
import com.example.couplecanvas.data.model.DailySpark
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DateVote
import com.example.couplecanvas.data.model.LoveNote
import com.example.couplecanvas.data.model.MemoryItem
import com.example.couplecanvas.data.model.LocationShareState
import com.example.couplecanvas.data.model.QuizAnswer
import com.example.couplecanvas.data.model.QuizDiscussion
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.SparkAnswer
import com.example.couplecanvas.util.DatePlanMatcher
import com.example.couplecanvas.util.QuizBank
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume

class FeatureRepository(private val firebase: FirebaseProvider) {
    fun observeNotes(roomId: String): Flow<List<LoveNote>> =
        firebase.root.child("rooms").child(roomId).child("notes").asListFlow()

    suspend fun sendNote(roomId: String, uid: String, message: String) {
        val id = UUID.randomUUID().toString()
        val note = LoveNote(id, uid, message.trim(), System.currentTimeMillis())
        firebase.root.child("rooms").child(roomId).child("notes").child(id).setValue(note).await()
        touchRoom(roomId)
    }

    suspend fun togglePin(roomId: String, note: LoveNote) {
        firebase.root.child("rooms").child(roomId).child("notes").child(note.noteId).child("isPinned").setValue(!note.isPinned).await()
    }

    suspend fun updateNote(roomId: String, noteId: String, message: String) {
        val trimmed = message.trim()
        require(trimmed.isNotEmpty()) { "노트 내용을 입력해주세요" }
        val now = System.currentTimeMillis()
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/notes/$noteId/message" to trimmed,
                "rooms/$roomId/notes/$noteId/updatedAt" to now,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
    }

    suspend fun markReceivedNotesRead(roomId: String, uid: String) {
        val notesSnapshot = firebase.root.child("rooms").child(roomId).child("notes").get().await()
        val updates = notesSnapshot.children.mapNotNull { noteSnapshot ->
            val note = noteSnapshot.getValue(LoveNote::class.java) ?: return@mapNotNull null
            val noteId = noteSnapshot.key ?: return@mapNotNull null
            if (note.authorUid != uid && !note.isRead) {
                "rooms/$roomId/notes/$noteId/isRead" to true
            } else {
                null
            }
        }.toMap()
        if (updates.isNotEmpty()) {
            firebase.root.updateChildren(updates).await()
        }
    }

    suspend fun deleteNote(roomId: String, noteId: String) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/notes/$noteId" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    fun observeDatePlans(roomId: String): Flow<List<DatePlan>> =
        firebase.root.child("rooms").child(roomId).child("datePlans").asListFlow()

    suspend fun saveDatePlan(roomId: String, plan: DatePlan) {
        firebase.root.child("rooms").child(roomId).child("datePlans").child(plan.planId).setValue(plan).await()
        touchRoom(roomId)
    }

    suspend fun updateDatePlan(roomId: String, plan: DatePlan) {
        val trimmedTitle = plan.title.trim()
        val trimmedDescription = plan.description.trim()
        require(trimmedTitle.isNotEmpty()) { "데이트 제목을 입력해주세요" }
        require(trimmedDescription.isNotEmpty()) { "데이트 설명을 입력해주세요" }
        val now = System.currentTimeMillis()
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/datePlans/${plan.planId}/title" to trimmedTitle,
                "rooms/$roomId/datePlans/${plan.planId}/description" to trimmedDescription,
                "rooms/$roomId/datePlans/${plan.planId}/vibe" to plan.vibe.trim().ifBlank { "Cozy" },
                "rooms/$roomId/datePlans/${plan.planId}/mode" to plan.mode,
                "rooms/$roomId/datePlans/${plan.planId}/tone" to plan.tone,
                "rooms/$roomId/datePlans/${plan.planId}/estimatedTime" to plan.estimatedTime?.trim()?.takeIf { it.isNotEmpty() },
                "rooms/$roomId/datePlans/${plan.planId}/estimatedBudget" to plan.estimatedBudget?.trim()?.takeIf { it.isNotEmpty() },
                "rooms/$roomId/datePlans/${plan.planId}/steps" to plan.steps.map { it.trim() }.filter { it.isNotEmpty() },
                "rooms/$roomId/datePlans/${plan.planId}/scheduledAt" to plan.scheduledAt,
                "rooms/$roomId/datePlans/${plan.planId}/updatedAt" to now,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
    }

    suspend fun updateDateStatus(roomId: String, planId: String, status: DatePlanStatus, scheduledAt: Long? = null) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>(
            "rooms/$roomId/datePlans/$planId/status" to status.value,
            "rooms/$roomId/datePlans/$planId/updatedAt" to now,
            "rooms/$roomId/updatedAt" to now,
        )
        if (scheduledAt != null) {
            updates["rooms/$roomId/datePlans/$planId/scheduledAt"] = scheduledAt
        }
        firebase.root.updateChildren(updates).await()
    }

    suspend fun voteDatePlan(roomId: String, planId: String, uid: String, vote: String) {
        val voteValue = DateVote.from(vote)?.value ?: throw IllegalArgumentException("알 수 없는 투표예요")
        val room = firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java)
            ?: throw FeatureRepositoryException("방을 찾지 못했어요")
        val memberIds = room.members.filterValues { it }.keys
        require(uid in memberIds) { "이 방에 참여한 사용자만 투표할 수 있어요" }

        val planRef = firebase.root.child("rooms").child(roomId).child("datePlans").child(planId)
        val initialRaw = planRef.get().await().value as? Map<*, *>
            ?: throw FeatureRepositoryException("데이트 플랜을 찾지 못했어요")
        val now = System.currentTimeMillis()
        var failure: Throwable? = null
        var usedInitialRaw = false
        val committed = suspendCancellableCoroutine<Boolean> { continuation ->
            planRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val raw = currentData.value as? Map<*, *> ?: run {
                        if (!usedInitialRaw) {
                            usedInitialRaw = true
                            initialRaw
                        } else {
                            failure = FeatureRepositoryException("데이트 플랜을 찾지 못했어요")
                            return Transaction.abort()
                        }
                    }
                    val updated = raw.entries.associate { it.key.toString() to it.value }.toMutableMap()
                    val votes = (raw["votes"] as? Map<*, *>)
                        .orEmpty()
                        .mapKeys { it.key.toString() }
                        .mapValues { it.value.toString() }
                        .toMutableMap()

                    votes[uid] = voteValue
                    val likedMemberIds = memberIds.filter { votes[it] == DateVote.Like.value }

                    updated["votes"] = votes
                    updated["updatedAt"] = now
                    if (likedMemberIds.size >= DatePlanMatcher.REQUIRED_MATCH_LIKES) {
                        val existingMatchedAt = (updated["matchedAt"] as? Number)?.toLong()?.takeIf { it > 0L }
                        updated["matchedAt"] = existingMatchedAt ?: now
                        updated["matchedBy"] = likedMemberIds.associateWith { true }
                    } else {
                        updated["matchedAt"] = null
                        updated["matchedBy"] = emptyMap<String, Boolean>()
                    }

                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: com.google.firebase.database.DatabaseError?,
                    committed: Boolean,
                    snapshot: com.google.firebase.database.DataSnapshot?,
                ) {
                    if (error != null) failure = FeatureRepositoryException("투표 저장에 실패했어요", error.toException())
                    continuation.resume(committed)
                }
            })
        }
        if (!committed) throw failure ?: FeatureRepositoryException("투표 저장에 실패했어요")
        touchRoom(roomId)
    }

    suspend fun deleteDatePlan(roomId: String, planId: String) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/datePlans/$planId" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    fun observeBucketItems(roomId: String): Flow<List<BucketItem>> =
        firebase.root.child("rooms").child(roomId).child("bucketList").asListFlow()

    suspend fun saveBucketItem(roomId: String, item: BucketItem) {
        firebase.root.child("rooms").child(roomId).child("bucketList").child(item.itemId).setValue(item).await()
        touchRoom(roomId)
    }

    suspend fun updateBucketItem(roomId: String, itemId: String, title: String, description: String?, vibe: String?) {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty()) { "버킷 제목을 입력해주세요" }
        val now = System.currentTimeMillis()
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/bucketList/$itemId/title" to trimmedTitle,
                "rooms/$roomId/bucketList/$itemId/description" to description?.trim()?.takeIf { it.isNotEmpty() },
                "rooms/$roomId/bucketList/$itemId/vibe" to vibe?.trim()?.takeIf { it.isNotEmpty() },
                "rooms/$roomId/bucketList/$itemId/updatedAt" to now,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
    }

    suspend fun planBucketItem(roomId: String, item: BucketItem, plan: DatePlan): DatePlan {
        val now = System.currentTimeMillis()
        val normalizedPlan = plan.copy(updatedAt = now)
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/datePlans/${normalizedPlan.planId}" to normalizedPlan,
                "rooms/$roomId/bucketList/${item.itemId}/status" to BucketStatus.Planned.value,
                "rooms/$roomId/bucketList/${item.itemId}/plannedDatePlanId" to normalizedPlan.planId,
                "rooms/$roomId/bucketList/${item.itemId}/updatedAt" to now,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
        return normalizedPlan
    }

    suspend fun updateBucketStatus(roomId: String, itemId: String, status: String) {
        firebase.root.child("rooms").child(roomId).child("bucketList").child(itemId).updateChildren(
            mapOf("status" to status, "updatedAt" to System.currentTimeMillis())
        ).await()
        touchRoom(roomId)
    }

    suspend fun deleteBucketItem(roomId: String, itemId: String) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/bucketList/$itemId" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    fun observeMemories(roomId: String): Flow<List<MemoryItem>> =
        firebase.root.child("rooms").child(roomId).child("memories").asListFlow()

    fun observeLocationShares(roomId: String): Flow<List<LocationShareState>> =
        firebase.root.child("rooms").child(roomId).child("locationShares").asListFlow()

    suspend fun saveMemory(roomId: String, uid: String, title: String, note: String?, date: Long, imageUris: List<Uri>): MemoryItem {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty()) { "추억 제목을 입력해주세요" }
        val memoryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val uploads = imageUris.take(4).mapIndexed { index, uri ->
            val path = "rooms/$roomId/uploads/$uid/memories/$memoryId/$index.jpg"
            uploadImage(path, uri)
        }
        val memory = MemoryItem(
            memoryId = memoryId,
            title = trimmedTitle,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            imageUrls = uploads.map { it.second },
            storagePaths = uploads.map { it.first },
            date = date,
            createdByUid = uid,
            createdAt = now,
        )
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/memories/$memoryId" to memory,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
        return memory
    }

    suspend fun updateMemory(roomId: String, memoryId: String, title: String, note: String?, date: Long) {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty()) { "추억 제목을 입력해주세요" }
        val now = System.currentTimeMillis()
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/memories/$memoryId/title" to trimmedTitle,
                "rooms/$roomId/memories/$memoryId/note" to note?.trim()?.takeIf { it.isNotEmpty() },
                "rooms/$roomId/memories/$memoryId/date" to date,
                "rooms/$roomId/memories/$memoryId/updatedAt" to now,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
    }

    suspend fun addMemoryImages(roomId: String, uid: String, memory: MemoryItem, imageUris: List<Uri>): MemoryItem {
        require(imageUris.isNotEmpty()) { "추가할 사진을 선택해주세요" }
        val optimisticRemainingSlots = MAX_MEMORY_IMAGES - memory.imageUrls.size
        require(optimisticRemainingSlots > 0) { "사진은 최대 ${MAX_MEMORY_IMAGES}장까지 저장할 수 있어요" }
        val uploads = imageUris.take(MAX_MEMORY_IMAGES).map { uri ->
            uploadImage(memoryImageUploadPath(roomId, uid, memory.memoryId), uri)
        }
        val memoryRef = firebase.root.child("rooms").child(roomId).child("memories").child(memory.memoryId)
        val initialMemory = memoryRef.get().await().getValue(MemoryItem::class.java)
            ?: throw FeatureRepositoryException("추억을 찾지 못했어요")
        val now = System.currentTimeMillis()
        var failure: Throwable? = null
        var updatedMemory: MemoryItem? = null
        var usedInitialMemory = false
        val committed = suspendCancellableCoroutine<Boolean> { continuation ->
            memoryRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentMemory = currentData.getValue(MemoryItem::class.java) ?: run {
                        if (!usedInitialMemory) {
                            usedInitialMemory = true
                            initialMemory
                        } else {
                            failure = FeatureRepositoryException("추억을 찾지 못했어요")
                            return Transaction.abort()
                        }
                    }
                    val remainingSlots = MAX_MEMORY_IMAGES - currentMemory.imageUrls.size
                    if (remainingSlots <= 0) {
                        failure = FeatureRepositoryException("사진은 최대 ${MAX_MEMORY_IMAGES}장까지 저장할 수 있어요")
                        return Transaction.abort()
                    }
                    val selectedUploads = uploads.take(remainingSlots)
                    currentData.value = currentMemory.copy(
                        imageUrls = currentMemory.imageUrls + selectedUploads.map { it.second },
                        storagePaths = currentMemory.storagePaths + selectedUploads.map { it.first },
                        updatedAt = now,
                    )
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: com.google.firebase.database.DatabaseError?,
                    committed: Boolean,
                    snapshot: com.google.firebase.database.DataSnapshot?,
                ) {
                    if (error != null) failure = FeatureRepositoryException("사진 추가에 실패했어요", error.toException())
                    updatedMemory = snapshot?.getValue(MemoryItem::class.java)
                    continuation.resume(committed)
                }
            })
        }
        val retainedStoragePaths = updatedMemory?.storagePaths.orEmpty().toSet()
        uploads
            .map { it.first }
            .filterNot { it in retainedStoragePaths }
            .forEach { path -> runCatching { firebase.storage.reference.child(path).delete().await() } }
        val committedMemory = updatedMemory
        if (!committed || committedMemory == null) {
            throw failure ?: FeatureRepositoryException("사진 추가에 실패했어요")
        }
        firebase.root.child("rooms").child(roomId).child("updatedAt").setValue(committedMemory.updatedAt).await()
        return committedMemory
    }

    suspend fun removeMemoryImage(roomId: String, memory: MemoryItem, imageIndex: Int): MemoryItem {
        require(imageIndex in memory.imageUrls.indices) { "삭제할 사진을 찾지 못했어요" }
        memory.storagePaths.getOrNull(imageIndex)?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { firebase.storage.reference.child(path).delete().await() }
        }
        val updatedMemory = memory.copy(
            imageUrls = memory.imageUrls.filterIndexed { index, _ -> index != imageIndex },
            storagePaths = memory.storagePaths.filterIndexed { index, _ -> index != imageIndex },
            updatedAt = System.currentTimeMillis(),
        )
        saveUpdatedMemory(roomId, updatedMemory)
        return updatedMemory
    }

    suspend fun deleteMemory(roomId: String, memory: MemoryItem) {
        memory.storagePaths.forEach { path ->
            runCatching { firebase.storage.reference.child(path).delete().await() }
        }
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/memories/${memory.memoryId}" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    fun observeQuizAnswers(roomId: String): Flow<List<QuizAnswer>> =
        firebase.root.child("rooms").child(roomId).child("quizAnswers").asListFlow()

    fun observeQuizDiscussions(roomId: String): Flow<List<QuizDiscussion>> =
        firebase.root.child("rooms").child(roomId).child("quizDiscussions").asListFlow()

    suspend fun answerQuiz(roomId: String, uid: String, questionId: String, answer: String, imageUri: Uri? = null) {
        val answerId = "$questionId-$uid"
        val upload = imageUri?.let { uri ->
            val path = "rooms/$roomId/uploads/$uid/quizzes/$answerId.jpg"
            uploadImage(path, uri)
        }
        val quizAnswer = QuizAnswer(
            answerId = answerId,
            questionId = questionId,
            uid = uid,
            answer = answer.trim(),
            imageUrl = upload?.second,
            storagePath = upload?.first,
            createdAt = System.currentTimeMillis(),
        )
        firebase.root.child("rooms").child(roomId).child("quizAnswers").child(answerId).setValue(quizAnswer).await()
        touchRoom(roomId)
    }

    suspend fun addQuizDiscussion(roomId: String, uid: String, questionId: String, message: String, imageUri: Uri? = null) {
        val trimmed = message.trim()
        require(trimmed.isNotEmpty() || imageUri != null) { "토론에 남길 내용을 입력해주세요" }
        val discussionId = UUID.randomUUID().toString()
        val upload = imageUri?.let { uri ->
            val path = "rooms/$roomId/uploads/$uid/quizDiscussions/$discussionId.jpg"
            uploadImage(path, uri)
        }
        val discussion = QuizDiscussion(
            discussionId = discussionId,
            questionId = questionId,
            authorUid = uid,
            message = trimmed,
            imageUrl = upload?.second,
            storagePath = upload?.first,
            createdAt = System.currentTimeMillis(),
        )
        firebase.root.child("rooms").child(roomId).child("quizDiscussions").child(discussionId).setValue(discussion).await()
        touchRoom(roomId)
    }

    fun observeDailySpark(roomId: String, dateKey: String): Flow<DailySpark?> =
        firebase.root.child("rooms").child(roomId).child("dailySparks").child(dateKey).asObjectFlow()

    fun observeDailySparks(roomId: String): Flow<List<DailySpark>> =
        firebase.root.child("rooms").child(roomId).child("dailySparks").asListFlow()

    suspend fun ensureDailySpark(roomId: String, dateKey: String): DailySpark {
        val ref = firebase.root.child("rooms").child(roomId).child("dailySparks").child(dateKey)
        val spark = DailySpark(
            sparkId = dateKey,
            question = QuizBank.dailySparkQuestion(dateKey),
            dateKey = dateKey,
        )
        var failure: Throwable? = null
        val committed = suspendCancellableCoroutine<Boolean> { continuation ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.value != null) return Transaction.success(currentData)
                    currentData.value = spark
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: com.google.firebase.database.DatabaseError?,
                    committed: Boolean,
                    snapshot: com.google.firebase.database.DataSnapshot?,
                ) {
                    if (error != null) failure = FeatureRepositoryException("Daily Spark 생성에 실패했어요", error.toException())
                    continuation.resume(committed)
                }
            })
        }
        if (!committed) throw failure ?: FeatureRepositoryException("Daily Spark 생성에 실패했어요")
        return ref.get().await().getValue(DailySpark::class.java) ?: spark
    }

    suspend fun answerDailySpark(roomId: String, dateKey: String, uid: String, answer: String) {
        val trimmed = answer.trim()
        require(trimmed.isNotEmpty()) { "Daily Spark 답변을 입력해주세요" }
        ensureDailySpark(roomId, dateKey)
        firebase.root.child("rooms").child(roomId).child("dailySparks").child(dateKey)
            .child("answers").child(uid).setValue(SparkAnswer(uid, trimmed, System.currentTimeMillis())).await()
        touchRoom(roomId)
    }

    suspend fun setLocationSharingEnabled(roomId: String, uid: String, enabled: Boolean) {
        val now = System.currentTimeMillis()
        val sharePath = "rooms/$roomId/locationShares/$uid"
        val updates = if (enabled) {
            mapOf(
                "$sharePath/uid" to uid,
                "$sharePath/enabled" to true,
                "$sharePath/consentedUids/$uid" to true,
                "$sharePath/isApproximateOnly" to true,
                "rooms/$roomId/updatedAt" to now,
            )
        } else {
            mapOf(
                "$sharePath/uid" to uid,
                "$sharePath/enabled" to false,
                "$sharePath/consentedUids/$uid" to false,
                "$sharePath/latitude" to null,
                "$sharePath/longitude" to null,
                "$sharePath/accuracyMeters" to null,
                "$sharePath/lastSharedAt" to now,
                "rooms/$roomId/updatedAt" to now,
            )
        }
        firebase.root.updateChildren(updates).await()
    }

    suspend fun shareCurrentLocation(
        roomId: String,
        uid: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        approximateOnly: Boolean,
    ) {
        val consentedMemberIds = requireMutualLocationConsent(roomId, uid)
        val now = System.currentTimeMillis()
        val share = LocationShareState(
            uid = uid,
            enabled = true,
            consentedUids = consentedMemberIds.associateWith { true },
            lastSharedAt = now,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            isApproximateOnly = approximateOnly,
        )
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/locationShares/$uid" to share,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
    }

    private suspend fun requireMutualLocationConsent(roomId: String, uid: String): Set<String> {
        val room = firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java)
            ?: throw FeatureRepositoryException("방을 찾지 못했어요")
        val memberIds = room.members.filterValues { it }.keys
            .ifEmpty { listOfNotNull(room.hostUid, room.guestUid).filter { it.isNotBlank() }.toSet() }
        if (uid !in memberIds) {
            throw FeatureRepositoryException("참여자만 가능")
        }
        if (memberIds.size < 2) {
            throw FeatureRepositoryException("상대방 대기")
        }

        val sharesSnapshot = firebase.root.child("rooms").child(roomId).child("locationShares").get().await()
        val localEnabled = sharesSnapshot.child(uid).child("enabled").getValue(Boolean::class.java) == true
        val partnerIds = memberIds - uid
        val partnerEnabled = partnerIds.all { partnerUid ->
            sharesSnapshot.child(partnerUid).child("enabled").getValue(Boolean::class.java) == true
        }
        if (!localEnabled) {
            throw FeatureRepositoryException("내 동의 필요")
        }
        if (!partnerEnabled) {
            throw FeatureRepositoryException("상대방 동의 필요")
        }
        return memberIds
    }

    private suspend fun touchRoom(roomId: String) {
        firebase.root.child("rooms").child(roomId).child("updatedAt").setValue(System.currentTimeMillis()).await()
    }

    private suspend fun saveUpdatedMemory(roomId: String, memory: MemoryItem) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/memories/${memory.memoryId}" to memory,
                "rooms/$roomId/updatedAt" to memory.updatedAt,
            ),
        ).await()
    }

    private fun memoryImageUploadPath(roomId: String, uid: String, memoryId: String): String =
        "rooms/$roomId/uploads/$uid/memories/$memoryId/${UUID.randomUUID()}.jpg"

    private suspend fun uploadImage(path: String, uri: Uri): Pair<String, String> =
        runCatching {
            val ref = firebase.storage.reference.child(path)
            val metadata = StorageMetadata.Builder()
                .setContentType(contentTypeForPath(path))
                .build()
            ref.putFile(uri, metadata).await()
            path to ref.downloadUrl.await().toString()
        }.getOrElse {
            throw FeatureRepositoryException("사진 업로드 실패", it)
        }

    private fun contentTypeForPath(path: String): String =
        when (path.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
}

class FeatureRepositoryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

private const val MAX_MEMORY_IMAGES = 4
