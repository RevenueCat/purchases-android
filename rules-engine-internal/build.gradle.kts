plugins {
    alias(libs.plugins.revenuecat.public.library)
}

android {
    namespace = "com.revenuecat.purchases.rules"
}

dependencies {
    // `org.json` ships with the Android runtime (and is the same artifact
    // backing `JSONObject` in production), so this only adds bytes to the
    // local JVM test classpath. Used exclusively by the test-only
    // `ValueJsonHelper` to express predicates as JSON literals.
    testImplementation(libs.json)
    testImplementation(libs.bundles.test)
}
