plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

apply(from = "$rootDir/base-application.gradle")

android {
    flavorDimensions += "apis"

    productFlavors {
        create("defaults") {
            dimension = "apis"
            isDefault = true
        }
        create("customEntitlementComputation") {
            dimension = "apis"
        }
    }

    defaultConfig {
        minSdk = 24 // RevenueCat UI requires 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "1024m"
            }
        }
    }

    namespace = "com.revenuecat.api_tester_kotlin"
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:amazon"))
    implementation(project(":ui:debugview"))
    implementation(project(":ui:revenuecatui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.compose.ui.google.fonts)
}
