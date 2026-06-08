package com.example.couplecanvas.data.repository

import com.example.couplecanvas.BuildConfig
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.JoinRoomResult
import com.example.couplecanvas.data.model.LoveNote
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.RoomHomeSummary
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.data.model.RoomUser
import com.example.couplecanvas.util.RoomCodeGenerator
import com.google.firebase.database.MutableData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

class RoomRepository(private val firebase: FirebaseProvider) {
    suspend fun createRoom(uid: String, title: String = "둘만의 그림방"): String {
        awaitAuthenticatedUid(uid)
        var lastCodeFailure: Throwable? = null
        repeat(20) {
            val now = System.currentTimeMillis()
            val roomId = UUID.randomUUID().toString()
            val roomCode = generateUniqueCode()
            val room = Room(
                roomId = roomId,
                roomCode = roomCode,
                title = title,
                createdAt = now,
                updatedAt = now,
                status = RoomStatus.Waiting.value,
                hostUid = uid,
                activeUserCount = 1,
                members = mapOf(uid to true),
            )
            val user = firebase.root.child("users").child(uid).get().await()
            val roomUser = RoomUser(
                uid = uid,
                role = "host",
                displayName = user.child("displayName").getValue(String::class.java),
                photoUrl = user.child("photoUrl").getValue(String::class.java),
                joinedAt = now,
                online = true,
                lastSeen = now,
            )
            firebase.root.updateChildren(
                mapOf(
                    "rooms/$roomId" to room.toFirebaseMap(users = mapOf(uid to roomUser)),
                    "userRooms/$uid/$roomId" to true,
                ),
            ).await()

            val codeResult = runCatching {
                firebase.root.child("roomCodes").child(roomCode).setValue(roomId).await()
            }
            if (codeResult.isSuccess) return roomId

            lastCodeFailure = codeResult.exceptionOrNull()
            runCatching {
                firebase.root.updateChildren(
                    mapOf(
                        "rooms/$roomId" to null,
                        "userRooms/$uid/$roomId" to null,
                    ),
                ).await()
            }
        }
        throw IllegalStateException("초대 코드를 예약하지 못했어요", lastCodeFailure)
    }

    suspend fun joinRoom(roomCode: String, uid: String): JoinRoomResult {
        val normalized = roomCode.trim().uppercase()
        if (!RoomCodeGenerator.isValid(normalized)) return JoinRoomResult.Error("6자리 초대 코드를 확인해주세요")
        awaitAuthenticatedUid(uid)
        val roomId = firebase.root.child("roomCodes").child(normalized).get().await().getValue(String::class.java)
            ?: return JoinRoomResult.NotFound

        val ref = firebase.root.child("rooms").child(roomId)
        val roomSnapshot = runCatching { ref.get().await() }.getOrElse {
            return JoinRoomResult.Full
        }
        val room = roomSnapshot.getValue(Room::class.java) ?: return JoinRoomResult.Full
        if (room.status == RoomStatus.Closed.value) return JoinRoomResult.Closed
        if (room.isMember(uid)) {
            updateJoinedMemberPresence(roomId, uid, room)
            return JoinRoomResult.Success(roomId)
        }

        val user = firebase.root.child("users").child(uid).get().await()
        val now = System.currentTimeMillis()
        val roomUser = RoomUser(
            uid = uid,
            role = "guest",
            displayName = user.child("displayName").getValue(String::class.java),
            photoUrl = user.child("photoUrl").getValue(String::class.java),
            joinedAt = now,
            online = true,
            lastSeen = now,
        )
        var outcome: JoinRoomResult = JoinRoomResult.Error("입장에 실패했어요")
        val committed = suspendCancellableCoroutine<Boolean> { continuation ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentRoom = currentData.getValue(Room::class.java) ?: run {
                        return Transaction.success(currentData)
                    }
                    if (currentRoom.status == RoomStatus.Closed.value) {
                        outcome = JoinRoomResult.Closed
                        return Transaction.abort()
                    }
                    if (currentRoom.isMember(uid)) {
                        val memberIds = currentRoom.activeMemberIds() + uid
                        currentData.child("members").child(uid).value = true
                        currentData.child("activeUserCount").value = memberIds.size.coerceAtMost(2)
                        currentData.child("status").value = if (memberIds.size >= 2) RoomStatus.Active.value else RoomStatus.Waiting.value
                        currentData.child("users").child(uid).value = roomUser.copy(
                            role = if (currentRoom.hostUid == uid) "host" else "guest",
                            joinedAt = now,
                        )
                        currentData.child("updatedAt").value = now
                        outcome = JoinRoomResult.AlreadyMember
                        return Transaction.success(currentData)
                    }
                    val memberIds = currentRoom.activeMemberIds()
                    if (!currentRoom.guestUid.isNullOrBlank() || memberIds.size >= 2) {
                        outcome = JoinRoomResult.Full
                        return Transaction.abort()
                    }
                    currentData.child("guestUid").value = uid
                    currentData.child("members").child(currentRoom.hostUid).value = true
                    currentData.child("members").child(uid).value = true
                    currentData.child("activeUserCount").value = 2
                    currentData.child("status").value = RoomStatus.Active.value
                    currentData.child("updatedAt").value = now
                    currentData.child("users").child(uid).value = roomUser
                    outcome = JoinRoomResult.Success(roomId)
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                    if (error != null) outcome = JoinRoomResult.Error(error.message)
                    continuation.resume(committed)
                }
            })
        }
        if (!committed) return outcome
        if (outcome is JoinRoomResult.Success || outcome is JoinRoomResult.AlreadyMember) {
            firebase.root.child("userRooms").child(uid).child(roomId).setValue(true).await()
            return JoinRoomResult.Success(roomId)
        }
        return outcome
    }

    suspend fun closeRoom(roomId: String, uid: String) {
        awaitAuthenticatedUid(uid)
        val room = firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java)
            ?: error("방을 찾지 못했어요")
        require(room.isMember(uid)) { "이 방을 관리할 권한이 없어요" }
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>(
            "rooms/$roomId/status" to RoomStatus.Closed.value,
            "rooms/$roomId/activeUserCount" to 0,
            "rooms/$roomId/updatedAt" to now,
            "roomCodes/${room.roomCode}" to null,
        )
        room.members.keys.forEach { memberUid ->
            updates["rooms/$roomId/users/$memberUid/online"] = false
            updates["rooms/$roomId/users/$memberUid/lastSeen"] = now
        }
        firebase.root.updateChildren(updates).await()
    }

    suspend fun reopenRoom(roomId: String, uid: String): String {
        awaitAuthenticatedUid(uid)
        val room = firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java)
            ?: error("방을 찾지 못했어요")
        require(room.isMember(uid)) { "이 방을 관리할 권한이 없어요" }
        val roomCode = usableRoomCode(room.roomCode, roomId)
        val memberCount = room.members.count { it.value }
        val now = System.currentTimeMillis()
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/roomCode" to roomCode,
                "rooms/$roomId/status" to if (memberCount >= 2) RoomStatus.Active.value else RoomStatus.Waiting.value,
                "rooms/$roomId/activeUserCount" to memberCount,
                "rooms/$roomId/updatedAt" to now,
            ),
        ).await()
        firebase.root.child("roomCodes").child(roomCode).setValue(roomId).await()
        return roomCode
    }

    fun observeRoom(roomId: String): Flow<Room?> =
        firebase.root.child("rooms").child(roomId).asObjectFlow()

    fun observeUsers(roomId: String): Flow<List<RoomUser>> =
        firebase.root.child("rooms").child(roomId).child("users").asListFlow()

    fun observeFirebaseConnection(): Flow<Boolean> =
        firebase.database
            .getReference(".info/connected")
            .asObjectFlow<Boolean>()
            .map { it == true }
            .distinctUntilChanged()

    fun observeRoomsForUser(uid: String): Flow<List<Room>> =
        observeRoomSummariesForUser(uid).map { summaries -> summaries.map { it.room } }

    fun observeRoomSummariesForUser(uid: String): Flow<List<RoomHomeSummary>> =
        callbackFlow {
            trySend(emptyList())
            val userRoomsRef = firebase.root.child("userRooms").child(uid)
            val roomListeners = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()
            var latestUserRoomsSnapshot: DataSnapshot? = null

            fun trackedRoomIds(snapshot: DataSnapshot): Set<String> =
                snapshot.children
                    .filter { it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                    .toSet()

            fun emitSummaries(snapshot: DataSnapshot) {
                launch {
                    trySend(loadRoomSummaries(uid, snapshot))
                }
            }

            fun syncRoomListeners(snapshot: DataSnapshot) {
                val nextIds = trackedRoomIds(snapshot)
                val removedIds = roomListeners.keys - nextIds
                removedIds.forEach { removedId ->
                    roomListeners.remove(removedId)?.let { (ref, listener) ->
                        ref.removeEventListener(listener)
                    }
                }
                nextIds.forEach { roomId ->
                    if (roomListeners.containsKey(roomId)) return@forEach
                    val roomRef = firebase.root.child("rooms").child(roomId)
                    val roomListener = object : ValueEventListener {
                        override fun onDataChange(roomSnapshot: DataSnapshot) {
                            latestUserRoomsSnapshot?.let(::emitSummaries)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            roomListeners.remove(roomId)?.let { (ref, listener) ->
                                ref.removeEventListener(listener)
                            }
                            latestUserRoomsSnapshot?.let(::emitSummaries)
                        }
                    }
                    roomListeners[roomId] = roomRef to roomListener
                    roomRef.addValueEventListener(roomListener)
                }
            }

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    latestUserRoomsSnapshot = snapshot
                    syncRoomListeners(snapshot)
                    emitSummaries(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    launch {
                        trySend(emptyList())
                    }
                }
            }
            userRoomsRef.addValueEventListener(listener)
            awaitClose {
                userRoomsRef.removeEventListener(listener)
                roomListeners.values.forEach { (ref, roomListener) ->
                    ref.removeEventListener(roomListener)
                }
                roomListeners.clear()
            }
        }

    private suspend fun loadRoomSummaries(uid: String, snapshot: com.google.firebase.database.DataSnapshot): List<RoomHomeSummary> {
        val ids = snapshot.children.filter { it.getValue(Boolean::class.java) == true }.mapNotNull { it.key }
        return ids.mapNotNull { id ->
            val roomSnapshot = runCatching {
                firebase.root.child("rooms").child(id).get().await()
            }.getOrNull() ?: return@mapNotNull null
            val room = roomSnapshot.getValue(Room::class.java) ?: return@mapNotNull null
            roomSnapshot.toHomeSummary(room, uid)
        }.sortedByDescending { it.room.updatedAt }
    }

    private fun com.google.firebase.database.DataSnapshot.toHomeSummary(room: Room, uid: String): RoomHomeSummary {
        val notes = child("notes").children.mapNotNull { it.getValue(LoveNote::class.java) }
        return RoomHomeSummary(
            room = room,
            noteCount = notes.size,
            unreadNoteCount = notes.count { it.authorUid != uid && !it.isRead },
            datePlanCount = child("datePlans").childrenCount.toInt(),
            memoryCount = child("memories").childrenCount.toInt(),
            drawingSnapshotCount = child("drawingSnapshots").childrenCount.toInt(),
            dailySparkCount = child("dailySparks").childrenCount.toInt(),
            bucketItemCount = child("bucketList").childrenCount.toInt(),
        )
    }

    suspend fun leaveRoom(roomId: String, uid: String) {
        awaitAuthenticatedUid(uid)
        val room = firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java)
            ?: error("방을 찾지 못했어요")
        val currentMembers = room.members
            .filterValues { it }
            .keys
            .toMutableSet()
            .apply {
                if (room.hostUid.isNotBlank()) add(room.hostUid)
                room.guestUid?.takeIf { it.isNotBlank() }?.let(::add)
            }
        require(uid in currentMembers) { "이 방에서 나갈 권한이 없어요" }

        val now = System.currentTimeMillis()
        val remainingMembers = currentMembers.filterNot { it == uid }
        val remainingMemberMap = remainingMembers.associateWith { true }
        val leavingHost = room.hostUid == uid
        val newHostUid = when {
            leavingHost -> remainingMembers.firstOrNull().orEmpty()
            room.hostUid.isNotBlank() && room.hostUid in remainingMembers -> room.hostUid
            else -> remainingMembers.firstOrNull().orEmpty()
        }
        val newGuestUid = remainingMembers.firstOrNull { it != newHostUid }
        val nextStatus = when {
            remainingMembers.isEmpty() -> RoomStatus.Closed.value
            room.status == RoomStatus.Closed.value -> RoomStatus.Closed.value
            remainingMembers.size >= 2 -> RoomStatus.Active.value
            else -> RoomStatus.Waiting.value
        }
        val nextActiveUserCount = if (nextStatus == RoomStatus.Closed.value) 0 else remainingMembers.size
        val keepInviteCode = remainingMembers.isNotEmpty() &&
            nextStatus != RoomStatus.Closed.value &&
            RoomCodeGenerator.isValid(room.roomCode)

        val updates = mutableMapOf<String, Any?>(
            "rooms/$roomId/updatedAt" to now,
            "rooms/$roomId/status" to nextStatus,
            "rooms/$roomId/activeUserCount" to nextActiveUserCount,
            "rooms/$roomId/hostUid" to newHostUid,
            "rooms/$roomId/guestUid" to newGuestUid,
            "rooms/$roomId/members" to remainingMemberMap,
            "rooms/$roomId/users/$uid/online" to false,
            "rooms/$roomId/users/$uid/lastSeen" to now,
            "rooms/$roomId/activeStrokes/$uid" to null,
            "rooms/$roomId/locationShares/$uid" to null,
            "userRooms/$uid/$roomId" to null,
            "roomCodes/${room.roomCode}" to if (keepInviteCode) roomId else null,
        )
        if (leavingHost && newHostUid.isNotBlank()) {
            updates["rooms/$roomId/users/$newHostUid/role"] = "host"
        }
        firebase.root.updateChildren(updates).await()
    }

    suspend fun updateOnlineStatus(roomId: String, uid: String, online: Boolean) {
        awaitAuthenticatedUid(uid)
        val now = System.currentTimeMillis()
        if (online) {
            firebase.root.child("rooms").child(roomId).child("users").child(uid).child("online").onDisconnect().setValue(false).await()
            firebase.root.child("rooms").child(roomId).child("users").child(uid).child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP).await()
        }
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/users/$uid/online" to online,
                "rooms/$roomId/users/$uid/lastSeen" to now,
            )
        ).await()
    }

    suspend fun updateRoomSettings(roomId: String, title: String, startedAt: Long?, privacyMode: Boolean) {
        awaitAuthenticatedUid(firebase.auth.currentUser?.uid.orEmpty())
        firebase.root.child("rooms").child(roomId).updateChildren(
            mapOf(
                "title" to title,
                "startedAt" to startedAt,
                "privacyMode" to privacyMode,
                "updatedAt" to System.currentTimeMillis(),
            )
        ).await()
    }

    private suspend fun generateUniqueCode(): String {
        repeat(20) {
            val code = RoomCodeGenerator.generate()
            val exists = firebase.root.child("roomCodes").child(code).get().await().exists()
            if (!exists) return code
        }
        error("초대 코드를 만들지 못했어요")
    }

    private suspend fun awaitAuthenticatedUid(uid: String) {
        val currentUser = firebase.auth.currentUser
        if (currentUser == null) {
            check(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
                "로그인이 필요해요"
            }
            awaitDatabaseConnection()
            return
        }
        require(currentUser.uid == uid) { "현재 로그인한 계정과 요청한 사용자가 달라요" }
        currentUser.getIdToken(false).await()
        awaitDatabaseConnection()
    }

    private suspend fun awaitDatabaseConnection() {
        withTimeoutOrNull(2_000) {
            suspendCancellableCoroutine { continuation ->
                val connectedRef = firebase.database.getReference(".info/connected")
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.getValue(Boolean::class.java) == true && continuation.isActive) {
                            connectedRef.removeEventListener(this)
                            continuation.resume(Unit)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) {
                            connectedRef.removeEventListener(this)
                            continuation.resume(Unit)
                        }
                    }
                }
                connectedRef.addValueEventListener(listener)
                continuation.invokeOnCancellation {
                    connectedRef.removeEventListener(listener)
                }
            }
        }
    }

    private suspend fun usableRoomCode(currentCode: String, roomId: String): String {
        if (RoomCodeGenerator.isValid(currentCode)) {
            val mappedRoomId = firebase.root.child("roomCodes").child(currentCode).get().await().getValue(String::class.java)
            if (mappedRoomId == null || mappedRoomId == roomId) return currentCode
        }
        return generateUniqueCode()
    }

    private fun Room.isMember(uid: String): Boolean =
        members[uid] == true || hostUid == uid || guestUid == uid

    private fun Room.activeMemberIds(): Set<String> =
        members.filterValues { it }.keys
            .plus(listOfNotNull(hostUid.takeIf { it.isNotBlank() }, guestUid?.takeIf { it.isNotBlank() }))
            .toSet()

    private suspend fun updateJoinedMemberPresence(roomId: String, uid: String, room: Room) {
        val now = System.currentTimeMillis()
        val user = firebase.root.child("users").child(uid).get().await()
        val existingRoomUser = firebase.root.child("rooms").child(roomId).child("users").child(uid).get().await().getValue(RoomUser::class.java)
        val memberIds = room.activeMemberIds() + uid
        val roomUser = RoomUser(
            uid = uid,
            role = existingRoomUser?.role?.takeIf { it == "host" || it == "guest" }
                ?: if (room.hostUid == uid) "host" else "guest",
            displayName = user.child("displayName").getValue(String::class.java) ?: existingRoomUser?.displayName,
            photoUrl = user.child("photoUrl").getValue(String::class.java) ?: existingRoomUser?.photoUrl,
            joinedAt = existingRoomUser?.joinedAt?.takeIf { it > 0L } ?: now,
            online = true,
            lastSeen = now,
        )
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/members/$uid" to true,
                "rooms/$roomId/status" to if (memberIds.size >= 2) RoomStatus.Active.value else RoomStatus.Waiting.value,
                "rooms/$roomId/activeUserCount" to memberIds.size.coerceAtMost(2),
                "rooms/$roomId/updatedAt" to now,
                "rooms/$roomId/users/$uid" to roomUser,
                "userRooms/$uid/$roomId" to true,
            ),
        ).await()
    }

    private fun Room.toFirebaseMap(users: Map<String, RoomUser>): Map<String, Any?> =
        mapOf(
            "roomId" to roomId,
            "roomCode" to roomCode,
            "title" to title,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "status" to status,
            "hostUid" to hostUid,
            "guestUid" to guestUid,
            "activeUserCount" to activeUserCount,
            "members" to members,
            "startedAt" to startedAt,
            "privacyMode" to privacyMode,
            "users" to users,
        )
}
