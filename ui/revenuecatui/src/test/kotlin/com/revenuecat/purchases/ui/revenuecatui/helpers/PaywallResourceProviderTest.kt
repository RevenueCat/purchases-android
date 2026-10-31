package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallResourceProviderTest {

    @Test
    fun `getLocales uses the host configuration instead of device preferences`() {
        val previousLocales = LocaleList.getDefault()
        try {
            LocaleList.setDefault(LocaleList(Locale.JAPAN))
            val configuration = Configuration().apply {
                setLocales(LocaleList(Locale.US, Locale.FRANCE))
            }
            val resources = mockk<Resources> {
                every { this@mockk.configuration } returns configuration
            }
            val provider = PaywallResourceProvider("TestApp", "com.test.app", resources)

            assertThat(provider.getLocales().toLanguageTags()).isEqualTo("en-US,fr-FR")
            assertThat(provider.getLocale()).isEqualTo(Locale.US)
        } finally {
            LocaleList.setDefault(previousLocales)
        }
    }

    @Test
    fun `getLocales falls back to all device preferences when the host has no locales`() {
        val previousLocales = LocaleList.getDefault()
        try {
            LocaleList.setDefault(LocaleList(Locale.JAPAN, Locale.US))
            val configuration = Configuration().apply { setLocales(LocaleList.getEmptyLocaleList()) }
            val resources = mockk<Resources> {
                every { this@mockk.configuration } returns configuration
            }
            val provider = PaywallResourceProvider("TestApp", "com.test.app", resources)

            assertThat(provider.getLocales()).isEqualTo(LocaleListCompat.getDefault())
            assertThat(provider.getLocales().toLanguageTags()).isEqualTo("ja-JP,en-US")
        } finally {
            LocaleList.setDefault(previousLocales)
        }
    }

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
}
