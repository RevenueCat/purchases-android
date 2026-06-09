package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.PredicateConformanceFixtureCase
import com.revenuecat.purchases.rules.helpers.PredicateConformanceFixtureLoader
import com.revenuecat.purchases.rules.helpers.PredicateConformanceRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Runs every in-repo JSON predicate fixture under
 * `src/test/resources/predicate-fixtures/` through the shared runner. One
 * parameterized case per fixture, named by its `id`.
 */
@RunWith(Parameterized::class)
internal class PredicateFixtureTest(
    private val fixture: PredicateConformanceFixtureCase,
) {

    @get:Rule
    val loggerRule = CapturingLoggerRule()

    @Test
    fun evaluatesPredicateFixture() {
        PredicateConformanceRunner.run(fixture, { loggerRule.warnings }, { loggerRule.logs })
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fixtures(): List<PredicateConformanceFixtureCase> =
            PredicateConformanceFixtureLoader.allCases
    }
}
