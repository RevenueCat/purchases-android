plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.revenuecat.rcttester"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.revenuecat.rcttester"
        minSdk = 24
        targetSdk = 36
        versionCode = (project.properties["rcTesterVersionCode"] as String).toInt()
        versionName = project.properties["rcTesterVersionName"] as String

        // Library modules have a dimension used to separate different APIs.
        // Applications don't need this, so we default to the "defaults" flavor.
        missingDimensionStrategy("apis", "defaults")

        flavorDimensions += "billingclient"

        productFlavors {
            create("bc8") {
                dimension = "billingclient"
                isDefault = true
            }
            create("bc7") {
                dimension = "billingclient"
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore")
            storePassword = project.properties["releaseKeystorePassword"] as String?
            keyAlias = project.properties["releaseKeyAlias"] as String?
            keyPassword = project.properties["releaseKeyPassword"] as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    // RevenueCat SDK
    implementation(project(":purchases"))
    implementation(project(":ui:revenuecatui"))

    // AndroidX Core
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore for configuration persistence
    implementation(libs.androidx.datastore.preferences)

    // Coroutines (included transitively via lifecycle-runtime-ktx, but explicit for clarity)
    implementation(libs.coroutines.core)

    // Serialization for configuration persistence
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.ui.tooling)
}
