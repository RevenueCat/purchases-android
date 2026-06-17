import java.util.Properties

plugins {
    alias(libs.plugins.revenuecat.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
    // Offline paywall snapshot tests (see src/test/.../PaywallSnapshotTest.kt).
    alias(libs.plugins.paparazzi)
}

// Paywall snapshot fixtures are recorded with the paywall-fixtures recorder plugin and committed under
// src/test/resources/revenuecat-paywall-fixtures. The recorder is NOT applied here so that a clean/CI
// build doesn't depend on the plugin being available in a repository (it resolves an external
// purchases-ui-testing coordinate that isn't in this build's dependency repos). External SDK consumers
// instead apply the published plugin, which auto-applies Paparazzi + the kit + testOptions in one step.
// To re-record after the dashboard changes, temporarily add the recorder and run recordPaywallFixtures:
//
//   buildscript {
//     repositories { mavenLocal(); mavenCentral() }
//     dependencies { classpath("com.revenuecat.purchases:purchases-paywall-fixtures-plugin:<version>") }
//   }
//   apply(plugin = "com.revenuecat.purchases.paywallfixtures")
//   extensions.configure<com.revenuecat.purchases.paywallfixtures.PaywallFixturesExtension>("paywallFixtures") {
//     offerings.set(setOf("alpha", "charlie", "delta", "echo", "foxtrot", "golf", "hotel"))
//   }
//
// This module already wires Paparazzi + the kit manually, so disable the plugin's auto-setup when
// recording (it would otherwise add an external purchases-ui-testing dependency not in this build's repos):
//
//   REVENUECAT_API_KEY=<public sdk key> ./gradlew :examples:paywall-tester:recordPaywallFixtures \
//       -Prevenuecat.paywallFixtures.snapshotTesting=false

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
    namespace = "com.revenuecat.paywallstester"

    defaultConfig {
        applicationId = "com.revenuecat.paywall_tester"
        minSdk = 24
        versionCode = (project.properties["paywallTesterVersionCode"] as String).toInt()
        versionName = project.properties["paywallTesterVersionName"] as String

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

        buildConfigField(
            "String",
            "PAYWALL_TESTER_API_KEY_A",
            "\"${resolveProperty("PAYWALL_TESTER_API_KEY_A")}\"",
        )
        buildConfigField(
            "String",
            "PAYWALL_TESTER_API_KEY_B",
            "\"${resolveProperty("PAYWALL_TESTER_API_KEY_B")}\"",
        )
        buildConfigField(
            "String",
            "PAYWALL_TESTER_API_KEY_A_LABEL",
            "\"${resolveProperty("PAYWALL_TESTER_API_KEY_A_LABEL")}\"",
        )
        buildConfigField(
            "String",
            "PAYWALL_TESTER_API_KEY_B_LABEL",
            "\"${resolveProperty("PAYWALL_TESTER_API_KEY_B_LABEL")}\"",
        )
        buildConfigField(
            "String",
            "PAYWALL_TESTER_AUTO_OPEN_OFFERING_ID",
            "\"${resolveProperty("PAYWALL_TESTER_AUTO_OPEN_OFFERING_ID")}\"",
        )
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
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("benchmark") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

baselineProfile {
    mergeIntoMain = true

    // Don't build on every iteration of a full assemble.
    // Instead enable generation directly for the release build variant.
    automaticGenerationDuringBuild = false

    // Make use of Dex Layout Optimizations via Startup Profiles
    dexLayoutOptimization = true
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:amazon"))
    implementation(project(":ui:debugview"))
    implementation(project(":ui:revenuecatui"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.compose.window.size)
    implementation(libs.navigation.compose)
    implementation(libs.compose.ui.google.fonts)
    implementation(libs.androidx.appcompat)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.test.compose.manifest)
    debugImplementation(libs.leakcanary.android)

    // Offline paywall snapshot testing (Paparazzi) via the testing kit, exercised exactly as an SDK
    // consumer would. Fixtures are recorded with the recordPaywallFixtures task above.
    testImplementation(project(":ui:revenuecatui-testing"))
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui)
    testImplementation(libs.assertJ)

    baselineProfile(project(":baselineprofile")) {
        attributes {
            attribute(
                Attribute.of("com.android.build.api.attributes.ProductFlavor:apis", String::class.java),
                "defaults",
            )
        }
    }
}
