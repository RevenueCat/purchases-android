package com.revenuecat.purchases.ui.revenuecatui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PaywallOptionsWebViewHandlerTest {

    private fun options(handler: PaywallWebViewMessageHandler?): PaywallOptions =
        PaywallOptions.Builder(dismissRequest = {})
            .setWebViewMessageHandler(handler)
            .build()

    @Test
    fun `builder stores the web view message handler`() {
        val handler = PaywallWebViewMessageHandler { _, _ -> }

        assertThat(options(handler).webViewMessageHandler).isSameAs(handler)
    }

    @Test
    fun `defaults to no web view message handler`() {
        val options = PaywallOptions.Builder(dismissRequest = {}).build()

        assertThat(options.webViewMessageHandler).isNull()
    }

    @Test
    fun `options differing only by handler are not equal`() {
        val handlerA = PaywallWebViewMessageHandler { _, _ -> }
        val handlerB = PaywallWebViewMessageHandler { _, _ -> }

        assertThat(options(handlerA)).isNotEqualTo(options(handlerB))
    }

    @Test
    fun `handler is excluded from hashCode so state updates are not triggered by it alone`() {
        val handlerA = PaywallWebViewMessageHandler { _, _ -> }
        val handlerB = PaywallWebViewMessageHandler { _, _ -> }

        // hashCode intentionally ignores the handler (like listener) so swapping handlers does not
        // force a paywall state refresh; see PaywallViewModel.updateOptions.
        assertThat(options(handlerA).hashCode()).isEqualTo(options(handlerB).hashCode())
    }
}
