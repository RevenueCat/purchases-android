plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.paparazzi)
}

if (!(project.properties["ANDROID_VARIANT_TO_PUBLISH"] as String).contains("customEntitlementComputation")) {
    apply(plugin = "com.vanniktech.maven.publish")
}

apply(from = "${rootProject.projectDir}/library.gradle")

android {
    namespace = "com.revenuecat.purchases.ui.debugview"

    flavorDimensions += "apis"
    productFlavors {
        create("defaults") {
            dimension = "apis"
            isDefault = true
        }
    }

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":purchases"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.test.compose.manifest)

    testImplementation(libs.androidx.appcompat)
    testImplementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.androidx.test.junit)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.androidx.test.compose)

    testImplementation(libs.assertJ)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)

    testImplementation(libs.androidx.legacy.core.ui)
}
