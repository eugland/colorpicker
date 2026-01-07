plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.primortex.color"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.primortex.color"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // CameraX
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")

    implementation("androidx.camera:camera-view:1.4.2")

    // Navigation Compose (if you want screen routing)
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Photo Picker (already in Activity, but keep updated)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)


    // Coil for image loading (photo pick screen)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(libs.volley)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.appcompat)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)                  // MockEngine
    testImplementation(libs.ktor.client.content.negotiation)   // needed by test HttpClient
    testImplementation(libs.ktor.serialization.kotlinx.json)   // needed by test HttpClient
    testImplementation(libs.kotlinx.coroutines.test)           // runTest()

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}