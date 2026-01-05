
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

dependencies {
    implementation(project(":purchases"))

    compileOnly("com.samsung.iap:samsung-iap:6.5.0@aar")
    testImplementation(libs.bundles.test)
    testImplementation("com.samsung.iap:samsung-iap:6.5.0@aar")
}

tasks.named("preBuild") {
    dependsOn(rootProject.tasks.named("getSamsungIapSdk"))
}
