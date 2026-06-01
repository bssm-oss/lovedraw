package com.example.couplecanvas

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseStorageRulesIntegrationTest {
    @Test
    fun storageSdkAllowsAuthenticatedSharedReadsAndKeepsWritesOwnerScoped() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()

                val owner = requireNotNull(firebase.auth.signInAnonymously().await().user)
                val ownerUid = owner.uid
                val roomId = "storage-rules-${UUID.randomUUID()}"
                val ownerPath = "rooms/$roomId/uploads/$ownerUid/memories/photo.jpg"
                val ownerRef = firebase.storage.reference.child(ownerPath)
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                ownerRef.putBytes(byteArrayOf(1, 2, 3), metadata).await()
                assertEquals("image/jpeg", ownerRef.metadata.await().contentType)

                firebase.auth.signOut()
                val partner = requireNotNull(firebase.auth.signInAnonymously().await().user)
                val partnerUid = partner.uid

                assertEquals("image/jpeg", ownerRef.metadata.await().contentType)

                val outsiderWriteFailure = runCatching {
                    ownerRef.putBytes(byteArrayOf(4, 5, 6), metadata).await()
                }.exceptionOrNull()
                assertTrue(
                    "Outsider write into another user's upload namespace should be rejected.",
                    outsiderWriteFailure.isStorageUnauthorized(),
                )

                val outsiderOwnRef = firebase.storage.reference
                    .child("rooms/$roomId/uploads/$partnerUid/memories/photo.jpg")
                outsiderOwnRef.putBytes(byteArrayOf(7, 8, 9), metadata).await()
                assertEquals("image/jpeg", outsiderOwnRef.metadata.await().contentType)

                outsiderOwnRef.delete().await()
                firebase.auth.signOut()
                firebase.auth.signInAnonymously().await()
                runCatching { ownerRef.delete().await() }
                firebase.auth.signOut()
            }
        }
    }

    private fun Throwable?.isStorageUnauthorized(): Boolean =
        this is StorageException && errorCode == StorageException.ERROR_NOT_AUTHORIZED
}
