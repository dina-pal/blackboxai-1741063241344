object Versions {
    // Kotlin and Gradle
    const val kotlin = "2.0.0"
    const val gradle = "8.2.0"
    
    // Core
    const val coreKtx = "1.12.0"
    const val lifecycle = "2.7.0"
    const val activityCompose = "1.8.2"
    
    // Compose
    const val compose = "1.6.0"
    const val composeMaterial3 = "1.2.0"
    const val composeCompiler = "1.5.8"
    const val navigationCompose = "2.7.6"
    
    // Koin
    const val koin = "3.5.3"
    const val koinCompose = "3.5.3"
    
    // Ktor
    const val ktor = "2.3.7"
    
    // Room
    const val room = "2.6.1"
    
    // Google Maps
    const val mapsCompose = "4.3.0"
    const val playServicesMaps = "18.2.0"
    const val playServicesLocation = "21.1.0"
    
    // Testing
    const val junit = "4.13.2"
    const val junitExt = "1.1.5"
    const val espresso = "3.5.1"
}

object Deps {
    // Kotlin
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    
    // Android
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.gradle}"
    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    const val lifecycleRuntimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
    const val activityCompose = "androidx.activity:activity-compose:${Versions.activityCompose}"
    
    // Compose
    const val composeBom = "androidx.compose:compose-bom:2024.01.00"
    const val composeUi = "androidx.compose.ui:ui"
    const val composeUiGraphics = "androidx.compose.ui:ui-graphics"
    const val composeUiToolingPreview = "androidx.compose.ui:ui-tooling-preview"
    const val composeMaterial3 = "androidx.compose.material3:material3:${Versions.composeMaterial3}"
    const val navigationCompose = "androidx.navigation:navigation-compose:${Versions.navigationCompose}"
    
    // Koin
    const val koinAndroid = "io.insert-koin:koin-android:${Versions.koin}"
    const val koinCompose = "io.insert-koin:koin-androidx-compose:${Versions.koinCompose}"
    
    // Ktor
    const val ktorCore = "io.ktor:ktor-client-core:${Versions.ktor}"
    const val ktorAndroid = "io.ktor:ktor-client-android:${Versions.ktor}"
    const val ktorContentNegotiation = "io.ktor:ktor-client-content-negotiation:${Versions.ktor}"
    const val ktorJson = "io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}"
    const val ktorLogging = "io.ktor:ktor-client-logging:${Versions.ktor}"
    
    // Room
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    
    // Google Maps
    const val mapsCompose = "com.google.maps.android:maps-compose:${Versions.mapsCompose}"
    const val playServicesMaps = "com.google.android.gms:play-services-maps:${Versions.playServicesMaps}"
    const val playServicesLocation = "com.google.android.gms:play-services-location:${Versions.playServicesLocation}"
    
    // Testing
    const val junit = "junit:junit:${Versions.junit}"
    const val junitExt = "androidx.test.ext:junit:${Versions.junitExt}"
    const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    const val composeUiTestJunit4 = "androidx.compose.ui:ui-test-junit4"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling"
    const val composeUiTestManifest = "androidx.compose.ui:ui-test-manifest"
}

object Plugins {
    const val application = "com.android.application"
    const val androidLibrary = "com.android.library"
    const val kotlinAndroid = "org.jetbrains.kotlin.android"
    const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
    const val ksp = "com.google.devtools.ksp"
}

object Android {
    const val minSdk = 26
    const val targetSdk = 35
    const val compileSdk = 35
    
    const val versionCode = 1
    const val versionName = "1.0.0"
    
    const val applicationId = "com.dinapal.busdakho"
}
