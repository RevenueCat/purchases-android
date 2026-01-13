plugins {
    id("revenuecat-public-library")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.poko)
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

val samsungIapVersion = libs.versions.samsungIap.get()

dependencies {
    implementation(project(":purchases"))

    compileOnly("com.samsung.iap:samsung-iap:$samsungIapVersion@aar")
    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlin.test)
    testImplementation("com.samsung.iap:samsung-iap:$samsungIapVersion@aar")
}
