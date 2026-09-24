plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    filename.set("api.txt")
}

android {
    namespace = "com.revenuecat.purchases.admob.nextgen"

    defaultConfig {
        missingDimensionStrategy("apis", "defaults")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":purchases"))
    compileOnly(libs.google.mobile.ads.next.gen.minimum)

    testImplementation(libs.bundles.test)
    testImplementation(libs.google.mobile.ads.next.gen)
}
