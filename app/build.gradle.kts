import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

fun setting(name: String, defaultValue: String): String =
    providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: env(name)
        ?: defaultValue

fun secretSetting(name: String): String? =
    providers.gradleProperty(name).orNull
        ?: keystoreProperties.getProperty(name)
        ?: env(name)

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseStoreFilePath = secretSetting("LOVEDRAW_RELEASE_STORE_FILE")
val releaseStorePassword = secretSetting("LOVEDRAW_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = secretSetting("LOVEDRAW_RELEASE_KEY_ALIAS")
val releaseKeyPassword = secretSetting("LOVEDRAW_RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() } && releaseStoreFilePath?.let { rootProject.file(it).isFile } == true

android {
    namespace = "com.example.couplecanvas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.couplecanvas"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "DATABASE_URL",
            setting("COUPLE_CANVAS_DATABASE_URL", "https://your-project-id-default-rtdb.firebaseio.com").asBuildConfigString(),
        )
        buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false")
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"10.0.2.2\"")
        buildConfigField("int", "FIREBASE_AUTH_EMULATOR_PORT", "9099")
        buildConfigField("int", "FIREBASE_DATABASE_EMULATOR_PORT", "9000")
        buildConfigField("int", "FIREBASE_STORAGE_EMULATOR_PORT", "9199")
        buildConfigField("String", "PRIVACY_POLICY_URL", setting("LOVEDRAW_PRIVACY_POLICY_URL", "").asBuildConfigString())
        buildConfigField("String", "ACCOUNT_DELETION_URL", setting("LOVEDRAW_ACCOUNT_DELETION_URL", "").asBuildConfigString())
        buildConfigField("String", "SUPPORT_EMAIL", setting("LOVEDRAW_SUPPORT_EMAIL", "").asBuildConfigString())
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    if (hasReleaseSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", setting("COUPLE_CANVAS_USE_FIREBASE_EMULATORS", "false"))
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = true
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
