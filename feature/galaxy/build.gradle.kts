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
}

// TO DO: Bring in Samsung SDK from somewhere else
val samsungIapAar: File? =
    sequenceOf(
        providers.gradleProperty("samsungIapSdkPath").orNull,
        providers.environmentVariable("SAMSUNG_IAP_SDK_PATH").orNull,
        rootProject.file("../samsung-iap-6.5.0.aar").takeIf { it.exists() }?.path,
    )
        .firstOrNull { !it.isNullOrBlank() }
        ?.let { path ->
            val aar = file(path)
            check(aar.exists()) {
                "Samsung IAP SDK AAR not found at $path. Override with samsungIapSdkPath property, " +
                    "SAMSUNG_IAP_SDK_PATH env var, or local.properties"
            }
            aar
        }

dependencies {
    implementation(project(":purchases"))

    implementation(files(samsungIapAar))
    testImplementation(libs.bundles.test)
}
