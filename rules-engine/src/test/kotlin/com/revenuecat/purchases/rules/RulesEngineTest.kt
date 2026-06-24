package com.revenuecat.purchases.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(InternalRulesEngineAPI::class)
class RulesEngineTest {

    @Test
    fun `RulesEngine namespace is reachable`() {
        // Smoke test: confirms the module is wired up and the test runner picks
        // it up. Real evaluation tests will land alongside the JSON Logic
        // implementation.
        assertThat(RulesEngine).isNotNull
    }
}
