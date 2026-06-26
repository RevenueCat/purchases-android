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

    @Test
    fun `getXmlFontFamily returns null instead of crashing when getXml returns null`() {
        // Reproduces the Paparazzi/layoutlib behavior: getXml returns null (rather than throwing
        // NotFoundException) for a font resource that can't be resolved. Before the fix, assigning the
        // null platform return to a non-null local crashed with a Kotlin intrinsic NPE.
        val resources = mockk<Resources>()
        every { resources.getXml(123) } returns null
        val provider = providerWith(resources)

        assertThat(provider.getXmlFontFamily(123)).isNull()
    }

    @Test
    fun `getXmlFontFamily returns null when the resource is not found`() {
        val resources = mockk<Resources>()
        every { resources.getXml(456) } throws Resources.NotFoundException("missing")
        val provider = providerWith(resources)

        assertThat(provider.getXmlFontFamily(456)).isNull()
    }

    @Test
    fun `getAssetFontPaths resolves fonts in the fonts dir`() {
        val provider = resourceProviderWith(mapOf("fonts" to arrayOf("myFont.ttf")))

        val paths = provider.getAssetFontPaths(listOf("myFont"))

        assertThat(paths).isEqualTo(mapOf("myFont" to "fonts/myFont.ttf"))
    }

    @Test
    fun `getAssetFontPaths resolves fonts in the public assets dir`() {
        val provider = resourceProviderWith(mapOf("public/assets" to arrayOf("capacitorFont.ttf")))

        val paths = provider.getAssetFontPaths(listOf("capacitorFont"))

        assertThat(paths).isEqualTo(mapOf("capacitorFont" to "public/assets/capacitorFont.ttf"))
    }

    @Test
    fun `getAssetFontPaths prefers fonts dir over public assets dir`() {
        val provider = resourceProviderWith(
            mapOf(
                "fonts" to arrayOf("sharedFont.ttf"),
                "public/assets" to arrayOf("sharedFont.ttf"),
            ),
        )

        val paths = provider.getAssetFontPaths(listOf("sharedFont"))

        assertThat(paths).isEqualTo(mapOf("sharedFont" to "fonts/sharedFont.ttf"))
    }

    @Test
    fun `getAssetFontPaths returns null when no font is found in any dir`() {
        val provider = resourceProviderWith(emptyMap())

        val paths = provider.getAssetFontPaths(listOf("missingFont"))

        assertThat(paths).isNull()
    }

    private fun providerWith(resources: Resources): PaywallResourceProvider =
        PaywallResourceProvider(
            applicationName = "App",
            packageName = "com.example",
            resources = resources,
        )

    private fun resourceProviderWith(
        assetsByDir: Map<String, Array<String>>,
    ): PaywallResourceProvider {
        val resources = mockk<Resources> {
            every { assets.list(any()) } returns emptyArray()
            assetsByDir.forEach { (dir, files) ->
                every { assets.list(dir) } returns files
            }
        }
        return PaywallResourceProvider(
            applicationName = "TestApp",
            packageName = "com.test.app",
            resources = resources,
        )
    }
}
