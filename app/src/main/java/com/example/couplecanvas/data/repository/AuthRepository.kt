package com.example.couplecanvas.data.repository

import com.example.couplecanvas.BuildConfig
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.AppUser
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val firebase: FirebaseProvider) {
    val currentUser: FirebaseUser?
        get() = firebase.auth.currentUser

    fun observeUser(): Flow<FirebaseUser?> = callbackFlow {
        trySend(firebase.auth.currentUser)
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebase.auth.addAuthStateListener(listener)
        awaitClose { firebase.auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebase.auth.signInWithCredential(credential).await()
        val user = requireNotNull(result.user)
        upsertUser(user)
        return user
    }

    suspend fun signInForDebugTest(displayName: String = "개발 테스트 사용자"): FirebaseUser {
        check(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
            "디버그 전용"
        }
        val result = firebase.auth.signInAnonymously().await()
        val user = requireNotNull(result.user)
        upsertUser(user, displayNameOverride = displayName)
        return user
    }

    suspend fun upsertUser(user: FirebaseUser, displayNameOverride: String? = null) {
        val now = System.currentTimeMillis()
        val ref = firebase.root.child("users").child(user.uid)
        val existing = ref.get().await().getValue(AppUser::class.java)
        val appUser = AppUser(
            uid = user.uid,
            displayName = displayNameOverride ?: user.displayName,
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
            createdAt = existing?.createdAt ?: now,
            lastSeen = now,
        )
        ref.setValue(appUser).await()
    }

    suspend fun signOut() {
        firebase.auth.signOut()
    }
}
