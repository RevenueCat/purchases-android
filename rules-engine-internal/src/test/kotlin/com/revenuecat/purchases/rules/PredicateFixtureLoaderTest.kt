package com.revenuecat.purchases.rules

import com.revenuecat.purchases.rules.helpers.PredicateConformanceFixtureLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Guards for the predicate fixture corpus itself, kept out of the
 * parameterized [PredicateFixtureTest] so they run once.
 */
internal class PredicateFixtureLoaderTest {

    @Test
    fun `fixtures load successfully`() {
        assertThat(PredicateConformanceFixtureLoader.allCases)
            .withFailMessage("Expected at least one in-repo predicate fixture")
            .isNotEmpty
    }

    @Test
    fun `fixture count matches expected`() {
        // Bump this when adding or removing fixtures. Guards against a fixture
        // file silently failing to load and shrinking the suite.
        val expectedCount = 345
        assertThat(PredicateConformanceFixtureLoader.allCases).hasSize(expectedCount)
    }
}
