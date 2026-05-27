plugins {
    alias(libs.plugins.revenuecat.public.library)
}

android {
    namespace = "com.revenuecat.purchases.rules"
}

dependencies {
    testImplementation(libs.bundles.test)
}
