plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val revenueCatApiKey: String = (project.findProperty("REVENUECAT_API_KEY") as? String)
    ?.takeIf { it.isNotBlank() && it != "must-inject" }
    ?: throw GradleException(
        """
        |
        |RevenueCat API key not configured!
        |
        |To configure the API key:
        |1. Open gradle.properties in the project root
        |2. Set your RevenueCat Google Play API key:
        |   REVENUECAT_API_KEY=goog_xxx
        |
        |Get your API key from: https://app.revenuecat.com
        |
        """.trimMargin(),
    )

val entitlementId: String? = (project.findProperty("REVENUECAT_ENTITLEMENT_ID") as? String)
    ?.takeIf { it.isNotBlank() }
    .also {
        if (it == null) {
            logger.warn(
                """
        |
        |WARNING: REVENUECAT_ENTITLEMENT_ID is not configured.
        |
        |To configure the entitlement ID:
        |1. Open gradle.properties in the project root
        |2. Set your RevenueCat entitlement ID:
        |   REVENUECAT_ENTITLEMENT_ID=your_entitlement_id
        |
        |Get your entitlement ID from: https://app.revenuecat.com
        |
                """.trimMargin(),
            )
        }
    }

val applicationIdProperty: String = (project.findProperty("APPLICATION_ID") as? String)
    ?.takeIf { it.isNotBlank() && it != "must-inject" }
    ?: throw GradleException(
        """
        |
        |Application ID not configured!
        |
        |To configure the application ID:
        |1. Open gradle.properties in the project root
        |2. Set your application ID (package name):
        |   APPLICATION_ID=com.example.yourapp
        |
        |This must match the package name in Google Play Console.
        |
        """.trimMargin(),
    )

android {
    namespace = "com.revenuecat.samplecat"
    compileSdk = 36

    defaultConfig {
        applicationId = applicationIdProperty
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatApiKey\"")
        buildConfigField("String", "ENTITLEMENT_ID", if (entitlementId != null) "\"$entitlementId\"" else "null")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Selects the SDK variant when CI builds against the local sources (-PuseLocalSdk).
        missingDimensionStrategy("apis", "defaults")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.purchases.ui)

    debugImplementation(libs.androidx.ui.tooling)
}
