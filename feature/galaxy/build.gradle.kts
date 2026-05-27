plugins {
    id("revenuecat-public-library")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.poko)
}

metalava {
    filename.set("api.txt")
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

dependencies {
    implementation(project(":purchases"))

    implementation(libs.samsung.iap)
    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlin.test)
}
