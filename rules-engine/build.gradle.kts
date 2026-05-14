plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    // Hide everything tagged with this module's internal-API marker so the
    // `scripts/check-rules-engine-internal-only.sh` guardrail can assert that
    // nothing leaks outside `@InternalRulesEngineAPI`. (The
    // `revenuecat-public-library` convention plugin already hides
    // `com.revenuecat.purchases.InternalRevenueCatAPI`.)
    hiddenAnnotations.add("com.revenuecat.purchases.rules.InternalRulesEngineAPI")
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Publishing-related wiring (Maven Central via `mavenPublishing { … }`, the
// `:bom` constraint, the `api.txt` metalava baseline + `scripts/api-dump.sh`
// entry, and dokka multi-module suppression) lives in a separate draft PR
// that flips the switch once the rules engine has functionality and a real
// consumer. Until then, this module just compiles, runs tests, and gets
// detekt'd on every PR. See the `:rules-engine` short-circuit in
// `ConfigureConditionalPublishing` in the `revenuecat-public-library`
// convention plugin.

dependencies {
    testImplementation(libs.bundles.test)
}
