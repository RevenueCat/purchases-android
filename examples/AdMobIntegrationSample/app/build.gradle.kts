plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    val compileVersion = 36
    compileSdk = compileVersion

    defaultConfig {
        applicationId = "com.revenuecat.sample.admob"
        minSdk = 26
        targetSdk = 36
        versionCode = rootProject.extra.get("versionCode")?.toString()?.toInt()
        versionName = rootProject.extra.get("versionName")?.toString()

        // Handle SDK product flavors
        missingDimensionStrategy("apis", "defaults")
        missingDimensionStrategy("billingclient", "bc8")

        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    namespace = "com.revenuecat.sample.admob"
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.cardview:cardview:1.0.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // RevenueCat
    implementation(libs.revenuecat)

    // AdMob
    implementation(libs.google.mobile.ads)
}
