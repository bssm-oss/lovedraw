package com.example.couplecanvas.data.firebase

import com.example.couplecanvas.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.storage.storage

class FirebaseProvider {
    val auth = Firebase.auth
    val database: FirebaseDatabase = FirebaseDatabase.getInstance(BuildConfig.DATABASE_URL)
    val storage = Firebase.storage

    init {
        configureEmulatorsIfNeeded()
    }

    val root: DatabaseReference = database.reference

    private fun configureEmulatorsIfNeeded() {
        if (!BuildConfig.USE_FIREBASE_EMULATORS) return
        database.setLogLevel(Logger.Level.WARN)
        auth.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, BuildConfig.FIREBASE_AUTH_EMULATOR_PORT)
        database.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, BuildConfig.FIREBASE_DATABASE_EMULATOR_PORT)
        database.goOnline()
        storage.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, BuildConfig.FIREBASE_STORAGE_EMULATOR_PORT)
    }
}
