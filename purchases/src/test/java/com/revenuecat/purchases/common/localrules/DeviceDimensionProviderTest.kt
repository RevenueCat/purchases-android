@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.LocaleProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as RobolectricConfig
import java.util.Date

@RunWith(AndroidJUnit4::class)
@RobolectricConfig(manifest = RobolectricConfig.NONE, sdk = [34])
class DeviceDimensionProviderTest {

    private val date = Date(1_700_000_000_000)
    private var languageTags = "en-US"

    private val localeProvider = object : LocaleProvider {
        override val currentLocalesLanguageTags: String
            get() = languageTags
    }

    @Test
    fun `provides the device dimensions`() = runTest {
        val dimensions = provider().dimensions(date)

        assertThat(dimensions).isEqualTo(
            mapOf(
                "app_version" to RulesDimensionValue.StringValue("1.2.3"),
                "locale" to RulesDimensionValue.StringValue("en_us"),
                "platform" to RulesDimensionValue.StringValue("android"),
                "platform_version" to RulesDimensionValue.StringValue("34"),
                "sdk_version" to RulesDimensionValue.StringValue(Config.frameworkVersion),
            ),
        )
    }

    @Test
    fun `platform reflects the store`() = runTest {
        val dimensions = provider(store = Store.AMAZON).dimensions(date)

        assertThat(dimensions["platform"]).isEqualTo(RulesDimensionValue.StringValue("amazon"))
    }

    @Test
    fun `only the preferred locale of the preference list is exposed`() = runTest {
        languageTags = "es-ES,en-US"

        val dimensions = provider().dimensions(date)

        assertThat(dimensions["locale"]).isEqualTo(RulesDimensionValue.StringValue("es_es"))
    }

    @Test
    fun `locale is re-read on every evaluation`() = runTest {
        val provider = provider()
        assertThat(provider.dimensions(date)["locale"]).isEqualTo(RulesDimensionValue.StringValue("en_us"))

        languageTags = "fr-FR"

        assertThat(provider.dimensions(date)["locale"]).isEqualTo(RulesDimensionValue.StringValue("fr_fr"))
    }

    @Test
    fun `locale falls back to the configured language tag when no locale is preferred`() = runTest {
        languageTags = ""

        val dimensions = provider(languageTag = "de-DE").dimensions(date)

        assertThat(dimensions["locale"]).isEqualTo(RulesDimensionValue.StringValue("de_de"))
    }

    @Test
    fun `dimensions the device has no value for are omitted`() = runTest {
        languageTags = ""

        val dimensions = provider(languageTag = "", versionName = "").dimensions(date)

        assertThat(dimensions).containsOnlyKeys("platform", "platform_version", "sdk_version")
    }

    private fun provider(
        store: Store = Store.PLAY_STORE,
        languageTag: String = "en-US",
        versionName: String = "1.2.3",
    ): DeviceDimensionProvider {
        val appConfig = mockk<AppConfig>().also {
            every { it.store } returns store
            every { it.languageTag } returns languageTag
            every { it.versionName } returns versionName
        }
        return DeviceDimensionProvider(appConfig, localeProvider)
    }
}
