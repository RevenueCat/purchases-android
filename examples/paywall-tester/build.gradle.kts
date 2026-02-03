plugins {
    id("revenuecat-android-application")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
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

    baselineProfile(project(":baselineprofile")) {
        attributes {
            attribute(
                Attribute.of("com.android.build.api.attributes.ProductFlavor:apis", String::class.java),
                "defaults",
            )
        }
    }
}
