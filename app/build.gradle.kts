plugins {
    alias(libs.plugins.android.application)
    // id("com.google.gms.google-services") // Disabled for local testing
}

android {
    namespace = "com.example.blottermanagementsystem"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.blottermanagementsystem"
        minSdk = 26
        targetSdk = 34
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
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.splashscreen)
    
    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // Lifecycle components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // Coroutines (for Room async operations)
    implementation(libs.kotlinx.coroutines.android)
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ZXing for QR Code
    implementation("com.google.zxing:core:3.5.2")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // MPAndroidChart for Analytics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // Firebase (DISABLED FOR LOCAL TESTING - Enable later for cloud)
    // implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // implementation("com.google.firebase:firebase-messaging")
    // implementation("com.google.firebase:firebase-analytics")
    // implementation("com.google.firebase:firebase-firestore")
    // implementation("com.google.firebase:firebase-storage")
    
    // CameraX (HEAVY - adds ~5MB)
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // ExoPlayer for Video (HEAVY - adds ~3MB)
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    
    // PDF Generation (HEAVY - adds ~2MB)
    implementation("com.itextpdf:itext7-core:7.2.5")
    
    // Lottie Animations (adds ~1MB)
    implementation("com.airbnb.android:lottie:6.2.0")
    
    // ML Kit for QR Scanning (HEAVY - adds ~5MB)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:vision-common:17.3.0")
    
    // WorkManager for Background Tasks
    implementation("androidx.work:work-runtime:2.9.0")
    
    // Paging 3 for Large Lists
    implementation("androidx.paging:paging-runtime:3.2.1")
    
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
