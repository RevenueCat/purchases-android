plugins {
    id("revenuecat-public-library")
}

android {
    namespace = "com.revenuecat.purchases.amazon"

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

    implementation(libs.amazon.appstore.sdk)
}
