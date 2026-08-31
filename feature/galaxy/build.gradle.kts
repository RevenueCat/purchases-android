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
