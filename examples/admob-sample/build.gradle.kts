import java.util.Properties

plugins {
    id("revenuecat-android-application")
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
        applicationId = "com.revenuecat.sample.admob"
        minSdk = 26
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "REVENUECAT_API_KEY",
            "\"${localProperties.getProperty("REVENUECAT_API_KEY", "")}\"",
        )

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

    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI",
        )
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    namespace = "com.revenuecat.sample.admob"
}

dependencies {
    // RevenueCat
    implementation(project(":purchases"))

    // RevenueCat AdMob Adapter
    implementation(project(":feature:admob"))

    // AndroidX
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation("androidx.cardview:cardview:1.0.0")

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // AdMob
    implementation(libs.google.mobile.ads)
}
