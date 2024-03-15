plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.revenuecat.testpurchasesandroidcompatibility"
    compileSdk = 33 // Keeping it at 33 to test compatibility with purchases-android

    defaultConfig {
        applicationId = "com.revenuecat.testpurchasesandroidcompatibility"
        minSdk = 19
        targetSdk = 33 // Keeping it at 33 to test compatibility with purchases-android
        versionCode = 1
        versionName = "1.0"

        missingDimensionStrategy("apis", "defaults")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:amazon"))

    implementation(libs.androidx.core)
    implementation(platform(libs.kotlin.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.multidex)
}
