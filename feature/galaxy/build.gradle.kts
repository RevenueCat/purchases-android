plugins {
    id("revenuecat-public-library")
}

android {
    namespace = "com.revenuecat.purchases.galaxy"

    // billingclient dimension is added for bc7/bc8 support
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
        missingDimensionStrategy("apis", "defaults")
    }

    testOptions {
        // TODO: Remove this?
        // Avoid merging Android manifests for JVM unit tests to prevent minSdk conflicts from optional AARs.
        unitTests.isIncludeAndroidResources = false
    }
}

val samsungIapSdkPath = providers.gradleProperty("samsungIapSdkPath")
    .orElse("/Users/willtaylor/Developer/sdks/SamsungInAppPurchaseSDK_v6.5.0/Libs/samsung-iap-6.5.0.aar")
    .map { path ->
        val aar = file(path)
        check(aar.exists()) {
            "Samsung IAP SDK AAR not found at $path. Override with -PsamsungIapSdkPath=/path/to/samsung-iap.aar"
        }
        aar
    }

dependencies {
    implementation(project(":purchases"))

    implementation(files(samsungIapSdkPath))
    testImplementation(libs.bundles.test)
}
