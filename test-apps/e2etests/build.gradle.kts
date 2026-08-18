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

fun resolveProperty(name: String, default: String): String {
    val projectProp = project.findProperty(name) as? String
    if (projectProp != null) return projectProp
    return localProperties.getProperty(name) ?: default
}

android {
    namespace = "com.revenuecat.e2etests"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.revenuecat.e2etests"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        missingDimensionStrategy("apis", "defaults")

        buildConfigField(
            "String",
            "WORKFLOWS_API_KEY",
            "\"${resolveProperty("E2E_WORKFLOWS_API_KEY", "workflows_api_key_to_replace")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":ui:debugview"))
    implementation(project(":ui:revenuecatui"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.test.compose.manifest)
}
