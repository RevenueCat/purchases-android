plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs)
}

apply(from = "$rootDir/base-application.gradle")

android {
    buildFeatures {
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.revenuecat.purchases_sample"
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

    namespace = "com.revenuecat.purchases_sample"
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:amazon"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.google.blockstore)
}
