
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.fyp.healthcare"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fyp.healthcare"
        minSdk = 24
        targetSdk = 37
        versionCode = 9
        versionName = "0.9.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8: shrink + optimize the release APK. Without this the release build runs
            // the same unoptimized bytecode as a debug build, which is a big part of why a
            // Compose app feels sluggish on a mid-range phone.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Compose-only app: it pulls in no fragment at all, but lint still flags
        // registerForActivityResult() in a plain ComponentActivity as if it were on an old
        // fragment version. The suggested "fix" is to add a fragment dependency the app
        // does not use, so the check is silenced instead.
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Sleep API
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Credential Manager
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    // Java time compatibility
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
