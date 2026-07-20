package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaywallWebViewVariablesProviderTest {

    @Test
    fun `sdkManagedVariables exposes only the locale system variable`() {
        val variables = PaywallWebViewVariablesProvider.sdkManagedVariables(locale = "en-US")

        // Only the locale system variable is exposed; nothing else is passed across the bridge in v1.
        assertThat(variables).hasSize(1)
        assertThat(variables[PaywallWebViewVariablesProvider.KEY_LOCALE]).isEqualTo("en-US")
    }
}
