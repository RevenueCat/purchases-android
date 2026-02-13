plugins {
    id("revenuecat-public-library")
}

android {
    namespace = "com.revenuecat.purchases.admob"

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

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":purchases"))
    compileOnly(libs.google.mobile.ads)

    testImplementation(libs.bundles.test)
    testImplementation(libs.google.mobile.ads)
}
