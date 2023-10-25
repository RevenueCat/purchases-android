package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.ui.text.font.FontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaywallOptionsTest {

    @Test
    fun `copy copies the same options but with the overwritten parameters`() {
        val options = PaywallOptions.Builder { }
            .setOfferingId("offeringId")
            .setShouldDisplayDismissButton(true)
            .setListener(object : PaywallListener {})
            .setFontProvider(CustomFontProvider(FontFamily.Default))
            .setMode(PaywallMode.footerMode(true))
            .build()
        val copy = options.copy(shouldDisplayDismissButton = false)
        assertThat(copy.offeringSelection).isEqualTo(options.offeringSelection)
        assertThat(copy.fontProvider).isEqualTo(options.fontProvider)
        assertThat(copy.listener).isEqualTo(options.listener)
        assertThat(copy.mode).isEqualTo(options.mode)
        assertThat(copy.shouldDisplayDismissButton).isFalse
    }
}
