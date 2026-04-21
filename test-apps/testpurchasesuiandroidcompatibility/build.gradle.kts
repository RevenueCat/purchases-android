plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.emerge)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.revenuecat.testpurchasesuiandroidcompatibility"
    compileSdk = 34 // Keeping at this level to test revenuecatui compatibility

    defaultConfig {
        applicationId = "com.revenuecat.testpurchasesuiandroidcompatibility"
        minSdk = 24
        targetSdk = 34 // Keeping at this level to test revenuecatui compatibility
        versionCode = 1
        versionName = "1.0"

        missingDimensionStrategy("apis", "defaults")
        missingDimensionStrategy("billingclient", "bc8")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

emerge {
    apiToken.set(System.getenv("EMERGE_API_TOKEN"))

    vcs {
        sha.set(Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "HEAD")).inputReader().readText().trim())
        branchName.set(
            Runtime.getRuntime()
                .exec(arrayOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
                .inputReader().readText().trim(),
        )
        val prNum = System.getenv("CIRCLE_PULL_REQUEST")
            .takeUnless { prUrl -> prUrl.isNullOrEmpty() }
            ?.takeIf { prUrl -> prUrl.contains('/') }
            ?.split('/')
            ?.lastOrNull()
            // Extract the PR number from the merge queue branch name.
            ?: "^gh-readonly-queue\\/([^/]+)\\/pr-(\\d+)-([0-9a-f]+)\$".toRegex(RegexOption.MULTILINE)
                .matchEntire(branchName.get())
                ?.groupValues
                ?.get(2)

        if (prNum != null) {
            prNumber.set(prNum)
        } else {
            // Don't set baseSha for main branch uploads
            baseSha.set(null as String?)
        }
        gitHub {
            repoName.set("purchases-android")
            repoOwner.set("RevenueCat")
        }
    }
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":ui:revenuecatui"))

    androidTestImplementation(libs.emerge.snapshots)

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
}
