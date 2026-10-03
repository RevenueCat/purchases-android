import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}
val releasedSdkVersion = providers.gradleProperty("SDK_UPDATE_TESTER_RELEASE_VERSION").orNull
val apiKey = providers.gradleProperty("MAESTRO_TEST_STORE_API_KEY")
    .orElse(providers.environmentVariable("MAESTRO_TEST_STORE_API_KEY"))
    .getOrElse(localProperties.getProperty("MAESTRO_TEST_STORE_API_KEY", "api_key_to_replace"))

android {
    namespace = "com.revenuecat.sdkupdatetester"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.revenuecat.SDKUpdateTester"
        minSdk = 24
        targetSdk = 36
        versionCode = if (releasedSdkVersion != null) 1 else 2
        versionName = "1.0"
        missingDimensionStrategy("apis", "defaults")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
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
        buildConfig = true
    }
}

dependencies {
    if (releasedSdkVersion != null) {
        implementation("com.revenuecat.purchases:purchases:$releasedSdkVersion")
    } else {
        implementation(project(":purchases"))
    }
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
