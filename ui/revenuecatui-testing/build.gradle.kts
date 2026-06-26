import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.revenuecat.public.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.revenuecat.purchases.ui.revenuecatui.testing"

    // billingclient dimension needed to match dependencies
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

    defaultConfig {
        minSdk = 24 // Matches :ui:revenuecatui
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Opt-in to InternalRevenueCatAPI for the entire module. This module is the public façade over the
// internal offline paywall rendering APIs. Lint is handled by ui/revenuecatui-testing/lint.xml.
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=com.revenuecat.purchases.InternalRevenueCatAPI")
    }
}

dependencies {
    api(project(":ui:revenuecatui"))
    // PaywallFixturesTestRule implements org.junit.rules.TestRule.
    api(libs.junit4)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.coroutines.test)

    testImplementation(libs.bundles.test)
}
