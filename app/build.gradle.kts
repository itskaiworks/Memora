plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.memora"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.memora"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")

    // Needed by MemoraViewModel (AndroidViewModel + viewModel() in Compose) — the class already
    // referenced these but the dependencies were never declared.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Needed by MainActivity / AddItemScreen / AddStorageScreen, which already call
    // collectAsStateWithLifecycle() — that extension lives in lifecycle-runtime-compose, not
    // lifecycle-runtime-ktx, and was missing, so those calls would not resolve at compile time.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Needed by MemoraDatabase — Room already referenced by MemoraViewModel/AddItemScreen but
    // never declared.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Needed by AddFlowComponents.kt / SavedScreen.kt, which already call coil3's AsyncImage.
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")

    // Live camera preview, capture and flash toggle (MemoraCamera.kt).
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // Needed for WorkManager periodic background tasks (Archive Purge)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}