import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.revenuecat.perftester"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.revenuecat.perftester"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        missingDimensionStrategy("apis", "defaults")

        buildConfigField(
            "String",
            "PERF_TESTER_API_KEY",
            "\"${localProperties.getProperty("PERF_TESTER_API_KEY", "")}\"",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":purchases"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.junit)
}
