plugins {
    id("revenuecat-public-library")
}

metalava {
    filename.set("api.txt")
}

android {
    namespace = "com.revenuecat.purchases.admob"

    defaultConfig {
        missingDimensionStrategy("billingclient", "bc8")
        missingDimensionStrategy("apis", "defaults")
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
