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
