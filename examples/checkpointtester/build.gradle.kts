plugins {
    alias(libs.plugins.revenuecat.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.revenuecat.checkpointtester"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        // Currently reusing the same applicationId as the paywall tester
        // for easier testing with the same store projects
        applicationId = "com.revenuecat.paywall_tester"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.test.compose.manifest)
    debugImplementation(libs.compose.ui.tooling)
}
