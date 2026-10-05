import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val revenueCatVersion = providers.gradleProperty("revenueCatVersion")
    .orNull
    ?: error("Pass -PrevenueCatVersion=<current VERSION_NAME> when building the compatibility app.")

android {
    namespace = "com.revenuecat.testapps.admoblegacycompatibility"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.revenuecat.testapps.admoblegacycompatibility"
        // Google Mobile Ads SDK (Legacy) 25.x requires API 24 or newer.
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.revenuecat.purchases:purchases:$revenueCatVersion")
    implementation("com.revenuecat.purchases:purchases-admob:$revenueCatVersion")

    // This direct dependency must stay newer than the adapter's minimum dependency so this app validates the
    // consumer configuration used by developers who adopt the latest Google Mobile Ads SDK (Legacy).
    implementation(libs.google.mobile.ads)
}
