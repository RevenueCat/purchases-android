import java.io.FileInputStream
import java.util.Properties

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

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localProperties.load(FileInputStream(localPropertiesFile))

// TODO: Bring the Samsung SDK in from somewhere else
val samsungIapSdkPath = providers.provider {
    providers.gradleProperty("samsungIapSdkPath").orNull
        ?: providers.environmentVariable("SAMSUNG_IAP_SDK_PATH").orNull
        ?: localProperties.getProperty("samsungIapSdkPath")
        ?: "/Users/willtaylor/Developer/sdks/SamsungInAppPurchaseSDK_v6.5.0/Libs/samsung-iap-6.5.0.aar"
}.map { path ->
    val aar = file(path)
    check(aar.exists()) {
        "Samsung IAP SDK AAR not found at $path. Override with samsungIapSdkPath property, SAMSUNG_IAP_SDK_PATH env var, or local.properties"
    }
    aar
}

dependencies {
    implementation(project(":purchases"))

    implementation(files(samsungIapSdkPath))
    testImplementation(libs.bundles.test)
}
