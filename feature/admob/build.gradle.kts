plugins {
    id("revenuecat-public-library")
}

android {
    namespace = "com.revenuecat.purchases.admob"

    defaultConfig {
        missingDimensionStrategy("billingclient", "bc8")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":purchases"))
    implementation(libs.google.mobile.ads)

    testImplementation(libs.bundles.test)
    testImplementation(libs.google.mobile.ads)
}
