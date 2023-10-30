package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.ui.text.font.FontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
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

    @Test
    fun `copy assigns null value to copy if copying with null value`() {
        val options = PaywallOptions.Builder { }
            .setOfferingId("offeringId")
            .setShouldDisplayDismissButton(true)
            .setListener(object : PaywallListener {})
            .setFontProvider(CustomFontProvider(FontFamily.Default))
            .setMode(PaywallMode.footerMode(true))
            .build()
        val copy = options.copy(listener = null)
        assertThat(copy.listener).isNull()
    }

    @Test
    fun `equals true if two different paywall options are equal`() {
        val onDismiss = {}
        val listener = object : PaywallListener {}
        val fontProvider = CustomFontProvider(FontFamily.Default)
        val options1 = PaywallOptions.Builder(onDismiss)
            .setOfferingId("offeringId")
            .setShouldDisplayDismissButton(true)
            .setListener(listener)
            .setFontProvider(fontProvider)
            .setMode(PaywallMode.footerMode(true))
            .build()
        val options2 = PaywallOptions.Builder(onDismiss)
            .setOfferingId("offeringId")
            .setShouldDisplayDismissButton(true)
            .setListener(listener)
            .setFontProvider(fontProvider)
            .setMode(PaywallMode.footerMode(true))
            .build()
        assertThat(options1).isEqualTo(options2)
    }

    @Test
    fun `equals false if two different onDismiss are used`() {
        val listener = object : PaywallListener {}
        val fontProvider = CustomFontProvider(FontFamily.Default)
        val options1 = PaywallOptions.Builder { /* Callback one */ }
            .setOfferingId("offeringId")
            .setShouldDisplayDismissButton(true)
            .setListener(listener)
            .setFontProvider(fontProvider)
            .setMode(PaywallMode.footerMode(true))
            .build()
        val options2 = PaywallOptions.Builder { /* Callback two */ }
            .setOfferingId("offeringId")
            .setShouldDisplayDismissButton(true)
            .setListener(listener)
            .setFontProvider(fontProvider)
            .setMode(PaywallMode.footerMode(true))
            .build()
        assertThat(options1).isNotEqualTo(options2)
    }
}
