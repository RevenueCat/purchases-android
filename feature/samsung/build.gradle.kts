plugins {
    id("revenuecat-public-library")
}

android {
    namespace = "com.revenuecat.purchases.samsung"

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
}
