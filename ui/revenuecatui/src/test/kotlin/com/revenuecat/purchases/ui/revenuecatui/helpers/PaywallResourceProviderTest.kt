package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallResourceProviderTest {

    private val resources = mockk<Resources>()
    private val provider = PaywallResourceProvider(
        applicationName = "App",
        packageName = "com.example",
        resources = resources,
    )

    @Test
    fun `getXmlFontFamily returns null instead of crashing when getXml returns null`() {
        // Reproduces the Paparazzi/layoutlib behavior: getXml returns null (rather than throwing
        // NotFoundException) for a font resource that can't be resolved. Before the fix, assigning the
        // null platform return to a non-null local crashed with a Kotlin intrinsic NPE.
        every { resources.getXml(123) } returns null

        assertThat(provider.getXmlFontFamily(123)).isNull()
    }

    @Test
    fun `getXmlFontFamily returns null when the resource is not found`() {
        every { resources.getXml(456) } throws Resources.NotFoundException("missing")

        assertThat(provider.getXmlFontFamily(456)).isNull()
    }
}
