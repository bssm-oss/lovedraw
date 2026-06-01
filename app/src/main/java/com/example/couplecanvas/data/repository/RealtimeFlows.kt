package com.example.couplecanvas.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

inline fun <reified T> DatabaseReference.asObjectFlow(): Flow<T?> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot.getValue(T::class.java))
        }

        override fun onCancelled(error: DatabaseError) {
            trySend(null)
            close()
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

inline fun <reified T> DatabaseReference.asListFlow(): Flow<List<T>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val items = snapshot.children.mapNotNull { it.getValue(T::class.java) }
            trySend(items)
        }

        override fun onCancelled(error: DatabaseError) {
            trySend(emptyList())
            close()
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}
