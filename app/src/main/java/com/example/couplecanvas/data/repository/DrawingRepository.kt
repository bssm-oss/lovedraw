package com.example.couplecanvas.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.CanvasEmoji
import com.example.couplecanvas.data.model.DrawingBackground
import com.example.couplecanvas.data.model.DrawingSnapshot
import com.example.couplecanvas.data.model.Stroke
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DrawingRepository(private val firebase: FirebaseProvider) {
    fun observeFinishedStrokes(roomId: String): Flow<List<Stroke>> =
        firebase.root.child("rooms").child(roomId).child("strokes").asListFlow()

    fun observeActiveStrokes(roomId: String): Flow<List<Stroke>> =
        firebase.root.child("rooms").child(roomId).child("activeStrokes").asListFlow()

    suspend fun getFinishedStrokes(roomId: String): List<Stroke> =
        firebase.root.child("rooms").child(roomId).child("strokes")
            .get()
            .await()
            .children
            .mapNotNull { it.getValue(Stroke::class.java) }

    suspend fun getActiveStrokes(roomId: String): List<Stroke> =
        firebase.root.child("rooms").child(roomId).child("activeStrokes")
            .get()
            .await()
            .children
            .mapNotNull { it.getValue(Stroke::class.java) }

    fun observeSnapshots(roomId: String): Flow<List<DrawingSnapshot>> =
        firebase.root.child("rooms").child(roomId).child("drawingSnapshots").asListFlow()

    fun observeEmojis(roomId: String): Flow<List<CanvasEmoji>> =
        firebase.root.child("rooms").child(roomId).child("emojis").asListFlow()

    fun observeBackground(roomId: String): Flow<DrawingBackground?> =
        firebase.root.child("rooms").child(roomId).child("drawingBackground").asObjectFlow()

    suspend fun updateActiveStroke(roomId: String, uid: String, stroke: Stroke) {
        firebase.root.child("rooms").child(roomId).child("activeStrokes").child(uid).setValue(stroke).await()
    }

    suspend fun finishStroke(roomId: String, uid: String, stroke: Stroke) {
        val updates = mapOf(
            "rooms/$roomId/strokes/${stroke.strokeId}" to stroke.copy(createdAt = stroke.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()),
            "rooms/$roomId/activeStrokes/$uid" to null,
            "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
        )
        firebase.root.updateChildren(updates).await()
    }

    suspend fun removeStroke(roomId: String, strokeId: String) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/strokes/$strokeId" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun deleteExpiredStrokes(roomId: String, uid: String, nowMillis: Long = System.currentTimeMillis()) {
        val strokesRef = firebase.root.child("rooms").child(roomId).child("strokes")
        val snapshot = strokesRef.get().await()
        val expiredOwnStrokeIds = snapshot.children.mapNotNull { child ->
            child.getValue(Stroke::class.java)
                ?.takeIf { it.ownerUid == uid && it.isExpired(nowMillis) }
                ?.let { child.key.orEmpty() }
        }
        if (expiredOwnStrokeIds.isEmpty()) return
        val updates = expiredOwnStrokeIds.associate { strokeId -> "rooms/$roomId/strokes/$strokeId" to null } +
            ("rooms/$roomId/updatedAt" to nowMillis)
        firebase.root.updateChildren(updates).await()
    }

    suspend fun clearCanvas(roomId: String) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/strokes" to null,
                "rooms/$roomId/activeStrokes" to null,
                "rooms/$roomId/emojis" to null,
                "rooms/$roomId/drawingBackground" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            )
        ).await()
    }

    suspend fun undoLastStroke(roomId: String, uid: String) {
        val strokesRef = firebase.root.child("rooms").child(roomId).child("strokes")
        val snapshot = strokesRef.get().await()
        val latest = snapshot.children.mapNotNull { child ->
            child.getValue(Stroke::class.java)?.takeIf { it.ownerUid == uid }?.let { child.key.orEmpty() to it }
        }.maxByOrNull { it.second.createdAt }
        latest?.let { strokesRef.child(it.first).removeValue().await() }
    }

    suspend fun addEmoji(roomId: String, uid: String, emoji: String, x: Float = 0.5f, y: Float = 0.5f, size: Float = 44f): CanvasEmoji {
        val id = UUID.randomUUID().toString()
        val canvasEmoji = CanvasEmoji(
            emojiId = id,
            ownerUid = uid,
            emoji = emoji,
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
            size = size.coerceIn(24f, 96f),
            createdAt = System.currentTimeMillis(),
        )
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/emojis/$id" to canvasEmoji,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
        return canvasEmoji
    }

    suspend fun undoLastEmoji(roomId: String, uid: String) {
        val emojisRef = firebase.root.child("rooms").child(roomId).child("emojis")
        val snapshot = emojisRef.get().await()
        val latest = snapshot.children.mapNotNull { child ->
            child.getValue(CanvasEmoji::class.java)?.takeIf { it.ownerUid == uid }?.let { child.key.orEmpty() to it }
        }.maxByOrNull { it.second.createdAt }
        latest?.let { emojisRef.child(it.first).removeValue().await() }
    }

    suspend fun setBackgroundImage(roomId: String, uid: String, uri: Uri): DrawingBackground {
        val id = UUID.randomUUID().toString()
        val path = "rooms/$roomId/uploads/$uid/backgrounds/$id.jpg"
        val ref = firebase.storage.reference.child(path)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putFile(uri, metadata).await()
        val background = DrawingBackground(
            imageUrl = ref.downloadUrl.await().toString(),
            storagePath = path,
            ownerUid = uid,
            updatedAt = System.currentTimeMillis(),
        )
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/drawingBackground" to background,
                "rooms/$roomId/updatedAt" to background.updatedAt,
            ),
        ).await()
        return background
    }

    suspend fun clearBackground(roomId: String) {
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/drawingBackground" to null,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun saveSnapshot(roomId: String, uid: String, bitmap: Bitmap, caption: String?, context: Context): DrawingSnapshot {
        val id = UUID.randomUUID().toString()
        val file = File(context.cacheDir, "$id.png")
        FileOutputStream(file).use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 96, stream) }
        val path = "rooms/$roomId/uploads/$uid/drawings/$id.png"
        val ref = firebase.storage.reference.child(path)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/png")
            .build()
        ref.putFile(Uri.fromFile(file), metadata).await()
        val url = ref.downloadUrl.await().toString()
        val snapshot = DrawingSnapshot(id, uid, url, path, System.currentTimeMillis(), caption)
        firebase.root.updateChildren(
            mapOf(
                "rooms/$roomId/drawingSnapshots/$id" to snapshot,
                "rooms/$roomId/updatedAt" to System.currentTimeMillis(),
            )
        ).await()
        return snapshot
    }
}
