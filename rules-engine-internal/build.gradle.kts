plugins {
    alias(libs.plugins.revenuecat.public.library)
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    testImplementation(libs.bundles.test)
}
