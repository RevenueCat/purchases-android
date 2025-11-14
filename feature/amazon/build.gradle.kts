plugins {
    id("revenuecat-public-library")
}

android {
    namespace = "com.revenuecat.purchases.amazon"

    defaultConfig {
        missingDimensionStrategy("apis", "defaults")
    }
}

dependencies {
    implementation(project(":purchases"))

    implementation(libs.amazon.appstore.sdk)
}
