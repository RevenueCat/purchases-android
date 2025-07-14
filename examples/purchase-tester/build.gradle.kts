plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs)
}

apply(from = "$rootDir/base-application.gradle")

android {
    defaultConfig {
        applicationId = "com.revenuecat.purchasetester"
        minSdk = (project.properties["purchaseTesterMinSdkVersion"] as String).toInt()
        versionCode = (project.properties["purchaseTesterVersionCode"] as String).toInt()
        versionName = project.properties["purchaseTesterVersionName"] as String
        vectorDrawables.useSupportLibrary = true

        // Library modules have a dimension used to separate different APIs.
        // Applications don't need this, so we default to the "defaults" flavor.
        missingDimensionStrategy("apis", "defaults")

        buildConfigField(
            "String",
            "SUPPORTED_STORES",
            "\"${project.properties["purchaseTesterSupportedStores"]}\"",
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
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
            testProguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-test-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    if (project.hasProperty("testBuildType")) {
        testBuildType = project.properties["testBuildType"] as String
    } else {
        testBuildType = "debug"
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
        viewBinding = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }

    namespace = "com.revenuecat.purchasetester"
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:amazon"))

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
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
}
