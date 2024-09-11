plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.emerge)
}

android {
    namespace = "com.revenuecat.testpurchasesuiandroidcompatibility"
    compileSdk = 34 // Keeping at this level to test revenuecatui compatibility

    defaultConfig {
        applicationId = "com.revenuecat.testpurchasesuiandroidcompatibility"
        minSdk = 24
        targetSdk = 34 // Keeping at this level to test revenuecatui compatibility
        versionCode = 1
        versionName = "1.0"

        missingDimensionStrategy("apis", "defaults")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

emerge {
    // TODO: RevenueCat to set from CircleCi variables
    apiToken.set(System.getenv("EMERGE_API_TOKEN"))

    vcs {
        sha.set(System.getenv("CIRCLE_SHA1"))
        val circleCiBaseSha = System.getenv("CIRCLE_MERGE_BASE")
        if (!circleCiBaseSha.isNullOrBlank()) {
            baseSha.set(circleCiBaseSha)
        } else {
            // Should skip setting for main branch uploads
            baseSha.set("")
        }
        gitHub {
            repoName.set("purchases-android")
            repoOwner.set("RevenueCat")
        }
    }
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":ui:revenuecatui"))

    androidTestImplementation(libs.emerge.snapshots)

    implementation(platform(libs.kotlin.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
}
