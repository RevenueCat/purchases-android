import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    filename.set("api.txt")
    // Hide everything tagged with this module's internal-API marker (in addition
    // to `com.revenuecat.purchases.InternalRevenueCatAPI`, which the
    // `revenuecat-public-library` convention plugin already hides).
    hiddenAnnotations.add("com.revenuecat.purchases.rules.InternalRulesEngineAPI")
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "defaultsRelease",
            sourcesJar = true,
            publishJavadocJar = true,
        ),
    )
}

dependencies {
    testImplementation(libs.bundles.test)
}
