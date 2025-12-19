plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.emerge)
}

android {
    namespace = "com.revenuecat.testapps.sdksizetesting"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.revenuecat.testapps.sdksizetesting"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.revenuecat)
    implementation(libs.revenuecat.ui)
}
