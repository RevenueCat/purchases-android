package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toNonEmptySetOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Date

@RunWith(Parameterized::class)
internal class PaywallStateLoadedComponentsLocaleTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args
) {

    class Args(
        val paywallLocales: NonEmptyList<String>,
        val deviceLocales: NonEmptyList<String>,
        val expected: String,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "single language",
                Args(
                    paywallLocales = nonEmptyListOf("en_US"),
                    deviceLocales = nonEmptyListOf("en-US"),
                    expected = "en-US",
                ),
            ),
            arrayOf(
                "prefer device",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "ja_JP"),
                    deviceLocales = nonEmptyListOf("ja-JP", "en-US"),
                    expected = "ja-JP",
                ),
            ),
            arrayOf(
                "paywall locale without region",
                Args(
                    paywallLocales = nonEmptyListOf("ja"),
                    deviceLocales = nonEmptyListOf("ja-JP"),
                    expected = "ja",
                ),
            ),
            arrayOf(
                "paywall locale without region, prefer device",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "ja"),
                    deviceLocales = nonEmptyListOf("ja-JP", "en-US"),
                    // We pick Japanese because the device prefers that, but we don't add the region as our paywall
                    // resources won't have a region either.
                    expected = "ja",
                ),
            ),
            arrayOf(
                "paywall locale without region, no matching device locale",
                Args(
                    paywallLocales = nonEmptyListOf("ja"),
                    deviceLocales = nonEmptyListOf("en-US"),
                    expected = "ja",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-Hans -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-Hans"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-CN -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-CN"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-SG -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-SG"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-MY -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-MY"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-CN -> zh-Hans-CN",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans", "zh_Hans_CN"),
                    deviceLocales = nonEmptyListOf("zh-CN"),
                    expected = "zh-Hans-CN",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-SG -> zh-Hans-SG",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans", "zh_Hans_SG"),
                    deviceLocales = nonEmptyListOf("zh-SG"),
                    expected = "zh-Hans-SG",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-MY -> zh-Hans-MY",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans", "zh_Hans_MY"),
                    deviceLocales = nonEmptyListOf("zh-MY"),
                    expected = "zh-Hans-MY",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-Hans-CN -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-Hans-CN"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-Hans-SG -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-Hans-SG"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "simplified Chinese zh-Hans-MY -> zh-Hans",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hans"),
                    deviceLocales = nonEmptyListOf("zh-Hans-MY"),
                    expected = "zh-Hans",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-Hant -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-Hant"),
                    expected = "zh-Hant",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-TW -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-TW"),
                    expected = "zh-Hant",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-HK -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-HK"),
                    expected = "zh-Hant",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-MO -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-MO"),
                    expected = "zh-Hant",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-TW -> zh-Hant-TW",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant", "zh_Hant_TW"),
                    deviceLocales = nonEmptyListOf("zh-TW"),
                    expected = "zh-Hant-TW",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-HK -> zh-Hant-HK",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant", "zh_Hant_HK"),
                    deviceLocales = nonEmptyListOf("zh-HK"),
                    expected = "zh-Hant-HK",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-MO -> zh-Hant-MO",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant", "zh_Hant_MO"),
                    deviceLocales = nonEmptyListOf("zh-MO"),
                    expected = "zh-Hant-MO",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-Hant-TW -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-Hant-TW"),
                    expected = "zh-Hant",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-Hant-HK -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-Hant-HK"),
                    expected = "zh-Hant",
                ),
            ),
            arrayOf(
                "traditional Chinese zh-Hant-MO -> zh-Hant",
                Args(
                    paywallLocales = nonEmptyListOf("en_US", "zh_Hant"),
                    deviceLocales = nonEmptyListOf("zh-Hant-MO"),
                    expected = "zh-Hant",
                ),
            ),
        )
    }

    @Test
    fun `Should properly determine locale`() {
        // Arrange
        val state = paywallState(
            paywallLocales = args.paywallLocales,
            deviceLocales = args.deviceLocales,
        )

        // Act
        val actual = state.locale.toLanguageTag()

        // Assert
        assertThat(actual).isEqualTo(args.expected)
    }

    private fun paywallState(
        paywallLocales: NonEmptyList<String>,
        deviceLocales: NonEmptyList<String>,
    ) = PaywallState.Loaded.Components(
        stack = previewStackComponentStyle(children = emptyList()),
        stickyFooter = null,
        background = BackgroundStyles.Color(color = ColorStyles(light = ColorStyle.Solid(Color.White))),
        showPricesWithDecimals = true,
        variableConfig = UiConfig.VariableConfig(),
        offering = Offering(
            identifier = "id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywall = null,
            paywallComponents = null,
        ),
        locales = paywallLocales.map { LocaleId(it) }.toNonEmptySetOrNull()!!,
        activelySubscribedProductIds = emptySet(),
        purchasedNonSubscriptionProductIds = emptySet(),
        dateProvider = { Date() },
        packages = PaywallState.Loaded.Components.AvailablePackages(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
        ),
        initialLocaleList = LocaleList(deviceLocales.map { Locale(it) }),
        initialSelectedTabIndex = 0,
    )

}
