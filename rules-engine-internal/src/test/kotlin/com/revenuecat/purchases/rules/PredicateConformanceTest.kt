package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.PredicateConformanceFixtureCase
import com.revenuecat.purchases.rules.helpers.PredicateConformanceFixtureLoader
import com.revenuecat.purchases.rules.helpers.PredicateConformanceRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Runs the khepri-generated audience predicate conformance fixtures through the
 * shared runner, mirroring iOS' `PredicateConformanceTests`. One parameterized
 * case per fixture, named by its `id`.
 *
 * The fixture envelope is downloaded on demand from the private RevenueCat/khepri
 * repo by `scripts/rules_engine/download_predicate_conformance_fixtures.sh` and is
 * git-ignored, so it is absent in normal checkouts. When absent there are no
 * parameters and the suite runs nothing; the default unit-test task also excludes
 * it (see the module `build.gradle.kts`). The dedicated CI job downloads the
 * fixtures and runs this suite explicitly.
 */
@RunWith(Parameterized::class)
internal class PredicateConformanceTest(
    private val fixture: PredicateConformanceFixtureCase,
) {

    @get:Rule
    val loggerRule = CapturingLoggerRule()

    @Test
    fun evaluatesConformanceFixture() {
        PredicateConformanceRunner.run(fixture, { loggerRule.warnings }, { loggerRule.logs })
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fixtures(): List<PredicateConformanceFixtureCase> =
            PredicateConformanceFixtureLoader.conformanceCasesOrEmpty()
    }
}

/**
 * Sanity check that the downloaded conformance envelope parses into at least one
 * fixture. Skipped (via a JUnit assumption) when the envelope is absent so it is
 * a no-op in normal checkouts, mirroring iOS' `fixturesLoadSuccessfully`.
 */
internal class PredicateConformanceFixturesTest {

    @Test
    fun fixturesLoadSuccessfully() {
        assumeTrue(
            "Predicate conformance fixtures not present; skipping",
            PredicateConformanceFixtureLoader.conformanceFixtureExists(),
        )
        assertThat(PredicateConformanceFixtureLoader.conformanceCases())
            .withFailMessage("Expected at least one khepri conformance fixture")
            .isNotEmpty
    }
}
