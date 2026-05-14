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
    // `org.json` ships with the Android runtime (and is the same artifact
    // backing `JSONObject` in production), so this only adds bytes to the
    // local JVM test classpath. Used exclusively by the test-only
    // `ValueJsonHelper` to express predicates as JSON literals.
    testImplementation(libs.json)
    testImplementation(libs.bundles.test)
}
