package com.revenuecat.purchases.ui.revenuecatui.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regression coverage for the font-neutralization the paywall snapshot testing kit relies on. Offline
 * fixture rendering must not depend on the consumer app's `res/font` resources (resolving them is
 * non-deterministic and crashes under layoutlib), so all fonts are forced to a system fallback.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FontFallbackResourceProviderTest {

    private val delegate = mockk<ResourceProvider>(relaxed = true)
    private val provider = FontFallbackResourceProvider(delegate)

    @Test
    fun `font resource identifiers are neutralized to zero so the resource-font path is skipped`() {
        every { delegate.getResourceIdentifier(any(), any()) } returns 999

        assertThat(provider.getResourceIdentifier("Roboto", "font")).isEqualTo(0)
    }

    @Test
    fun `non-font resource identifiers are delegated`() {
        every { delegate.getResourceIdentifier("ic_logo", "drawable") } returns 42

        assertThat(provider.getResourceIdentifier("ic_logo", "drawable")).isEqualTo(42)
    }

    @Test
    fun `xml font family always resolves to null`() {
        assertThat(provider.getXmlFontFamily(123)).isNull()
    }

    @Test
    fun `downloaded fonts are not resolved`() {
        val fontInfo = mockk<UiConfig.AppConfig.FontsConfig.FontInfo.Name>()

        assertThat(provider.getCachedFontFamilyOrStartDownload(fontInfo)).isNull()
    }

    @Test
    fun `non-font calls are delegated`() {
        every { delegate.getApplicationName() } returns "My App"

        assertThat(provider.getApplicationName()).isEqualTo("My App")
    }
}
