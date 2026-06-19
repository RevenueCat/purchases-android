package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaywallWebViewVariablesProviderTest {

    @Test
    fun `sdkManagedVariables includes locale and color scheme`() {
        val variables = PaywallWebViewVariablesProvider.sdkManagedVariables(
            locale = "en-US",
            colorScheme = WebViewColorScheme.DARK,
            customVariables = emptyMap(),
        )

        assertThat(variables[PaywallWebViewVariablesProvider.KEY_LOCALE])
            .isEqualTo(PaywallWebViewValue.String("en-US"))
        assertThat(variables[PaywallWebViewVariablesProvider.KEY_COLOR_SCHEME])
            .isEqualTo(PaywallWebViewValue.String("dark"))
    }

    @Test
    fun `sdkManagedVariables nests custom variables under custom`() {
        val variables = PaywallWebViewVariablesProvider.sdkManagedVariables(
            locale = "en-US",
            colorScheme = WebViewColorScheme.LIGHT,
            customVariables = mapOf(
                "plan" to CustomVariableValue.String("annual"),
                "level" to CustomVariableValue.Number(42),
                "premium" to CustomVariableValue.Boolean(true),
            ),
        )

        val custom = variables[PaywallWebViewVariablesProvider.KEY_CUSTOM] as PaywallWebViewValue.Object
        assertThat(custom.value["plan"]).isEqualTo(PaywallWebViewValue.String("annual"))
        assertThat(custom.value["level"]).isEqualTo(PaywallWebViewValue.Number(42))
        assertThat(custom.value["premium"]).isEqualTo(PaywallWebViewValue.Boolean(true))
    }

    @Test
    fun `color scheme unknown maps to unknown`() {
        val variables = PaywallWebViewVariablesProvider.sdkManagedVariables(
            locale = "en-US",
            colorScheme = WebViewColorScheme.UNKNOWN,
            customVariables = emptyMap(),
        )

        assertThat(variables[PaywallWebViewVariablesProvider.KEY_COLOR_SCHEME])
            .isEqualTo(PaywallWebViewValue.String("unknown"))
    }

    @Test
    fun `sanitizeAppProvidedVariables drops reserved keys`() {
        val sanitized = PaywallWebViewVariablesProvider.sanitizeAppProvidedVariables(
            mapOf(
                "locale" to PaywallWebViewValue.String("zz-ZZ"),
                "color_scheme" to PaywallWebViewValue.String("neon"),
                "custom" to PaywallWebViewValue.Object(
                    mapOf("app_segment" to PaywallWebViewValue.String("high_intent")),
                ),
            ),
        )

        assertThat(sanitized).doesNotContainKeys("locale", "color_scheme")
        assertThat(sanitized).containsKey("custom")
    }
}
