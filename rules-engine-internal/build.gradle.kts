import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.revenuecat.public.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.revenuecat.purchases.rules"
}

dependencies {
    // `org.json` is provided by the Android platform for the main source set;
    // this dependency adds a real implementation to the JVM unit-test classpath
    // (which exercises the production `ValueJson` parser).
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.test)
}

// The khepri predicate conformance suite depends on a fixture file downloaded on
// demand from the private RevenueCat/khepri repo (git-ignored, absent by default).
// The dedicated conformance CI job (and local runs) opt in with
// `-PrunRulesEngineConformanceTests`.
val runRulesEngineConformanceTests = providers.gradleProperty("runRulesEngineConformanceTests").isPresent
val conformanceFixtureFile = layout.projectDirectory.file(
    "src/test/resources/predicate-conformance/predicate_conformance_v1.json",
)

if (runRulesEngineConformanceTests) {
    // Mirror iOS' Xcode scheme pre-action: fetch the fixtures before the suite runs so
    // the conformance tests can be run locally with a single command. Both this task and
    // the test JVM point at the same absolute path via the env var the script honors.
    val downloadPredicateConformanceFixtures by tasks.registering(Exec::class) {
        description = "Downloads the khepri predicate conformance fixtures."
        group = "verification"
        workingDir = rootProject.projectDir
        environment("KHEPRI_PREDICATE_CONFORMANCE_FIXTURE_PATH", conformanceFixtureFile.asFile.absolutePath)
        commandLine("./scripts/rules_engine/download_predicate_conformance_fixtures.sh")
    }
    tasks.withType<Test>().configureEach {
        dependsOn(downloadPredicateConformanceFixtures)
        environment("KHEPRI_PREDICATE_CONFORMANCE_FIXTURE_PATH", conformanceFixtureFile.asFile.absolutePath)
    }
} else {
    // Keep it out of the standard unit-test run.
    tasks.withType<Test>().configureEach {
        filter {
            isFailOnNoMatchingTests = false
            excludeTestsMatching("com.revenuecat.purchases.rules.PredicateConformanceTest")
            excludeTestsMatching("com.revenuecat.purchases.rules.PredicateConformanceFixturesTest")
        }
    }
}
