plugins {
    id("revenuecat-api-tester-application")
    alias(libs.plugins.compose.compiler)
}

android {
    defaultConfig {
        minSdk = 24 // RevenueCat UI requires 24
        missingDimensionStrategy("billingclient", "bc8")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:amazon"))
    defaultsImplementation(project(":feature:admob"))
    defaultsImplementation(libs.google.mobile.ads)
    implementation(project(":ui:debugview"))
    implementation(project(":ui:revenuecatui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.compose.ui.google.fonts)
}
