import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.revenuecat.public.library)
    alias(libs.plugins.kotlin.serialization)
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
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.test)
}

// The khepri predicate conformance suite depends on a fixture file downloaded on
// demand from the private RevenueCat/khepri repo (git-ignored, absent by default).
// Keep it out of the standard unit-test run; the dedicated conformance CI job opts
// in with `-PrunRulesEngineConformanceTests`.
val runRulesEngineConformanceTests = providers.gradleProperty("runRulesEngineConformanceTests").isPresent
if (!runRulesEngineConformanceTests) {
    tasks.withType<Test>().configureEach {
        filter {
            isFailOnNoMatchingTests = false
            excludeTestsMatching("com.revenuecat.purchases.rules.PredicateConformanceTest")
            excludeTestsMatching("com.revenuecat.purchases.rules.PredicateConformanceFixturesTest")
        }
    }
}
