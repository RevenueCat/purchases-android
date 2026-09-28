package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.getFormatted
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptySetOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class CurrencyLocaleForStorefrontTests {

    @Test
    fun `excludes POSIX locale with different currency spacing`() {
        val availableLocales = arrayOf(
            Locale.US,
            Locale.forLanguageTag("en-Latn-US"),
            Locale.forLanguageTag("en-US-POSIX"),
        )

        val result = requireNotNull(
            getAvailableStorefrontCountryLocalesByLanguage(
                storefrontCountryCode = "US",
                availableLocales = availableLocales,
            )["en"],
        )

        assertThat(result).isEqualTo(Locale.forLanguageTag("en-Latn-US"))
        val formattedPrice = NumberFormat.getCurrencyInstance(result).apply {
            currency = Currency.getInstance("USD")
        }.format(1.99)
        assertThat(formattedPrice).isEqualTo("$1.99")
    }

    @Test
    fun `preserves existing selection behavior for non-POSIX variants`() {
        val valencian = Locale.forLanguageTag("ca-ES-VALENCIA")

        val result = getAvailableStorefrontCountryLocalesByLanguage(
            storefrontCountryCode = "ES",
            availableLocales = arrayOf(Locale.forLanguageTag("ca-ES"), valencian),
        )["ca"]

        assertThat(result).isEqualTo(valencian)
    }

    @Test
    fun `keeps paywall language when storefront has no matching locale`() {
        assertThat(getAvailableStorefrontCountryLocalesByLanguage("IL")).doesNotContainKey("es")

        val state = paywallState("es_ES", "IL", "es-ES")

        assertThat(state.currencyLocale.toLanguageTag()).isEqualTo("es-IL")
        val formattedPrice = Price("", 149_900_000, "ILS").getFormatted(state.currencyLocale.toJavaLocale())
        assertThat(formattedPrice).contains("149")
        assertThat(formattedPrice).doesNotContain("١", "٤", "٩")
    }

    @Test
    fun `preserves storefront currency symbol when number formats match`() {
        val state = paywallState("en_US", "JP", "en-US")

        val formattedPrice = Price("", 149_000_000, "JPY").getFormatted(state.currencyLocale.toJavaLocale())
        assertThat(formattedPrice).isEqualTo("￥149")
    }

    private fun paywallState(
        paywallLocale: String,
        storefrontCountryCode: String,
        deviceLocale: String,
    ): PaywallState.Loaded.Components =
        PaywallState.Loaded.Components(
            stack = previewStackComponentStyle(children = emptyList()),
            header = null,
            stickyFooter = null,
            background = BackgroundStyles.Color(color = ColorStyles(light = ColorStyle.Solid(Color.White))),
            showPricesWithDecimals = true,
            variableConfig = UiConfig.VariableConfig(
                variableCompatibilityMap = emptyMap(),
                functionCompatibilityMap = emptyMap(),
            ),
            variableDataProvider = VariableDataProvider(MockResourceProvider()),
            offering = Offering(
                identifier = "id",
                serverDescription = "description",
                metadata = emptyMap(),
                availablePackages = emptyList(),
                paywall = null,
                paywallComponents = null,
            ),
            locales = nonEmptySetOf(LocaleId(paywallLocale)),
            storefrontCountryCode = storefrontCountryCode,
            dateProvider = { Date() },
            packages = PaywallState.Loaded.Components.AvailablePackages(
                packagesOutsideTabs = emptyList(),
                packagesByTab = emptyMap(),
            ),
            initialLocaleList = LocaleList(deviceLocale),
            purchases = MockPurchasesType(),
        )
}
