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

// Maven Central publishing wiring (the `mavenPublishing { … }` block, the
// `scripts/api-dump.sh` entry that keeps `api.txt` enforced in CI, the
// `:bom` constraint, the revert of the `ConfigureConditionalPublishing`
// short-circuit, and the `dokkaHtmlPartial` suppression) lives in a
// separate draft PR that flips the switch once the rules engine has
// functionality and a real consumer. Until then, this module just
// compiles, runs tests, and gets detekt'd on every PR.

dependencies {
    // `org.json` ships with the Android runtime (and is the same artifact
    // backing `JSONObject` in production), so this only adds bytes to the
    // local JVM test classpath. Used exclusively by the test-only
    // `ValueJsonHelper` to express predicates as JSON literals.
    testImplementation(libs.json)
    testImplementation(libs.bundles.test)
}
