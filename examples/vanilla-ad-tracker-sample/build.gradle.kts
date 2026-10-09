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

android {
    defaultConfig {
        applicationId = "com.revenuecat.sample.vanilla"
        minSdk = 26
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "REVENUECAT_API_KEY",
            "\"${localProperties.getProperty("REVENUECAT_API_KEY", "")}\"",
        )
        buildConfigField(
            "String",
            "LEVELPLAY_APP_KEY",
            "\"${localProperties.getProperty("LEVELPLAY_APP_KEY", "25b63cf85")}\"",
        )
        buildConfigField(
            "String",
            "LEVELPLAY_BANNER_AD_UNIT_ID",
            "\"${localProperties.getProperty("LEVELPLAY_BANNER_AD_UNIT_ID", "4fpetq4lhe5lsw3e")}\"",
        )
        buildConfigField(
            "String",
            "LEVELPLAY_INTERSTITIAL_AD_UNIT_ID",
            "\"${localProperties.getProperty("LEVELPLAY_INTERSTITIAL_AD_UNIT_ID", "h3xw38h9214adgxo")}\"",
        )
        buildConfigField(
            "String",
            "LEVELPLAY_REWARDED_AD_UNIT_ID",
            "\"${localProperties.getProperty("LEVELPLAY_REWARDED_AD_UNIT_ID", "syz3d8ekts22q0or")}\"",
        )

        // Library modules have dimensions used to separate different APIs and billing client versions.
        // Applications don't need this, so we default to the "defaults" flavor.
        missingDimensionStrategy("apis", "defaults")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    namespace = "com.revenuecat.sample.vanilla"
}

dependencies {
    // RevenueCat
    implementation(project(":purchases"))

    // AndroidX
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // LevelPlay APIs are referenced directly in sample source files.
    implementation(libs.levelplay)
    implementation(libs.levelplay.unity.ads.adapter)
    implementation(libs.unity.ads)
    implementation(libs.levelplay.ads.identifier)
    implementation(libs.levelplay.app.set)
}
