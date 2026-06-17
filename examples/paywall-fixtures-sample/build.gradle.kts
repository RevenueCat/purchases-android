// Simulates an SDK consumer using the paywall snapshot testing kit. It deliberately uses none of this
// repo's convention plugins, and depends on the kit through testImplementation only — proving that the
// kit's public API surface is sufficient for snapshot-testing paywalls in a regular app/library module.
//
// A real consumer would also apply the fixtures recorder plugin and record their own paywalls:
//
//     plugins {
//         id("com.revenuecat.purchases.paywallfixtures")
//     }
//
//     // REVENUECAT_API_KEY=<public sdk key> ./gradlew recordPaywallFixtures
//
// That plugin is a sibling module here (it isn't resolvable by id within this build), so this sample
// uses fixtures checked into src/test/resources/revenuecat-paywall-fixtures instead.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.revenuecat.paywallfixturessample"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        missingDimensionStrategy("apis", "defaults")
        missingDimensionStrategy("billingclient", "bc8")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
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

dependencies {
    testImplementation(project(":ui:revenuecatui-testing"))
    // For mavenLocal e2e verification, comment the line above and use the published coordinates instead:
    // testImplementation("com.revenuecat.purchases:purchases-ui-testing:10.9.0-SNAPSHOT")

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui)

    testImplementation(libs.assertJ)
    testImplementation(libs.coroutines.test)
}
