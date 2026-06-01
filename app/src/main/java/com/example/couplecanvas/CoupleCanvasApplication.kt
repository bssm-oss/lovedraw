package com.example.couplecanvas

import android.app.Application
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.local.OverlayStateStore
import com.example.couplecanvas.data.local.WidgetStateStore
import com.example.couplecanvas.data.repository.AuthRepository
import com.example.couplecanvas.data.repository.DrawingRepository
import com.example.couplecanvas.data.repository.FeatureRepository
import com.example.couplecanvas.data.repository.RoomRepository
import com.google.firebase.FirebaseApp

class CoupleCanvasApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val firebaseProvider = FirebaseProvider()
    val widgetStateStore = WidgetStateStore(application)
    val overlayStateStore = OverlayStateStore(application)
    val authRepository = AuthRepository(firebaseProvider)
    val roomRepository = RoomRepository(firebaseProvider)
    val drawingRepository = DrawingRepository(firebaseProvider)
    val featureRepository = FeatureRepository(firebaseProvider)
}
