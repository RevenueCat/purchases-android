package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class WebViewUrlResolverTest {

    @Test
    fun `resolves static https url unchanged`() {
        val result = resolve("https://paywalls.revenuecat.com/index.html")

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/index.html")
    }

    @Test
    fun `resolves static https url with characters accepted by WebView`() {
        val result = resolve("https://paywalls.revenuecat.com/index.html?filters=a|b")

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/index.html?filters=a|b")
    }

    @Test
    fun `resolves runtime custom variable`() {
        val result = resolve(
            template = "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
            customVariables = mapOf("animal" to CustomVariableValue.String("dog")),
        )

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/dog.html")
    }

    @Test
    fun `falls back to dashboard default custom variable`() {
        val result = resolve(
            template = "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
            defaultCustomVariables = mapOf("animal" to CustomVariableValue.String("cat")),
        )

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/cat.html")
    }

    @Test
    fun `runtime custom variable overrides dashboard default`() {
        val result = resolve(
            template = "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
            customVariables = mapOf("animal" to CustomVariableValue.String("bird")),
            defaultCustomVariables = mapOf("animal" to CustomVariableValue.String("cat")),
        )

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/bird.html")
    }

    @Test
    fun `supports custom variable functions`() {
        val result = resolve(
            template = "https://paywalls.revenuecat.com/{{ custom.animal | lowercase }}.html",
            customVariables = mapOf("animal" to CustomVariableValue.String("DOG")),
        )

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/dog.html")
    }

    @Test
    fun `missing custom variable can still resolve to valid url`() {
        val result = resolve("https://paywalls.revenuecat.com/{{ custom.missing }}.html")

        assertThat(result?.toString()).isEqualTo("https://paywalls.revenuecat.com/.html")
    }

    @Test
    fun `returns null for malformed url`() {
        val result = resolve("not a url")

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for malformed https url`() {
        val result = resolve("https:///missing-host")

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for non https url`() {
        val result = resolve(
            template = "http://paywalls.revenuecat.com/{{ custom.animal }}.html",
            customVariables = mapOf("animal" to CustomVariableValue.String("dog")),
        )

        assertThat(result).isNull()
    }

    private fun resolve(
        template: String,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
        defaultCustomVariables: Map<String, CustomVariableValue> = emptyMap(),
    ) = WebViewUrlResolver.resolve(
        urlTemplate = template,
        variableConfig = UiConfig.VariableConfig(),
        customVariables = customVariables,
        defaultCustomVariables = defaultCustomVariables,
        locale = Locale.US,
    )
}
