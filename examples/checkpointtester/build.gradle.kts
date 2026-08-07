import java.util.Properties

plugins {
    alias(libs.plugins.revenuecat.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

fun resolveProperty(name: String, default: String = ""): String {
    val projectProp = project.findProperty(name) as? String
    if (projectProp != null) return projectProp
    return localProperties.getProperty(name) ?: default
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

        // Library modules have a dimension used to separate different APIs.
        // Applications don't need this, so we default to the "defaults" flavor.
        missingDimensionStrategy("apis", "defaults")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Defaults to the paywall tester's key since both apps share the same applicationId.
        buildConfigField(
            "String",
            "CHECKPOINT_TESTER_API_KEY",
            "\"${resolveProperty(
                "CHECKPOINT_TESTER_API_KEY",
            ).ifEmpty { resolveProperty("PAYWALL_TESTER_API_KEY_A") }}\"",
        )
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
    implementation(project(":purchases"))
    implementation(project(":ui:revenuecatui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.navigation.compose)
    debugImplementation(libs.androidx.test.compose.manifest)
    debugImplementation(libs.compose.ui.tooling)
}
