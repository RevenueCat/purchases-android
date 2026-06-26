package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
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
        assertThat(variables[PaywallWebViewVariablesProvider.KEY_LOCALE])
            .isEqualTo(PaywallWebViewValue.String("en-US"))
    }

    @Test
    fun `sanitizeAppProvidedVariables drops reserved keys`() {
        val sanitized = PaywallWebViewVariablesProvider.sanitizeAppProvidedVariables(
            mapOf(
                "locale" to PaywallWebViewValue.String("zz-ZZ"),
                "app_segment" to PaywallWebViewValue.String("high_intent"),
            ),
        )

        assertThat(sanitized).doesNotContainKey("locale")
        assertThat(sanitized).containsKey("app_segment")
    }
}
