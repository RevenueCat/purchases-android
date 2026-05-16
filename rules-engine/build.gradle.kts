plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    // The signature dump lands under `build/` (gitignored) and is consumed by
    // `scripts/check-rules-engine-internal-only.sh`, which asserts that nothing
    // public leaks outside `@InternalRulesEngineAPI`. `enforceCheck` is left
    // off because there is no committed baseline to diff against — the check
    // is intrinsic, not baseline-based.
    filename.set(layout.buildDirectory.file("api-dump.txt").get().asFile.path)
    hiddenAnnotations.add("com.revenuecat.purchases.rules.InternalRulesEngineAPI")
    enforceCheck.set(false)
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // `org.json` ships with the Android runtime (and is the same artifact
    // backing `JSONObject` in production), so this only adds bytes to the
    // local JVM test classpath. Used exclusively by the test-only
    // `ValueJsonHelper` to express predicates as JSON literals.
    testImplementation(libs.json)
    testImplementation(libs.bundles.test)
}
