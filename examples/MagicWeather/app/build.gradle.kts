plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    val compileVersion = 34
    compileSdk = compileVersion

    defaultConfig {
        applicationId = "com.revenuecat.purchases_sample"
        minSdk = 26
        targetSdk = compileVersion
        versionCode = rootProject.extra.get("versionCode")?.toString()?.toInt()
        versionName = rootProject.extra.get("versionName")?.toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "store"
    productFlavors {
        create("amazon") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"amazon\"")
        }

        create("google") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"google\"")
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
        buildConfig = true
    }
    namespace = "com.revenuecat.sample"
}

dependencies {
    implementation(libs.revenuecat)
    implementation(libs.revenuecat.amazon)
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
}
