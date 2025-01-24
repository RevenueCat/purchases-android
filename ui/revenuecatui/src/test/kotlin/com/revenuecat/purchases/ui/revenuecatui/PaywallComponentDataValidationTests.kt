package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.AllLocalizationsMissing
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.MissingStringLocalization
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class PaywallComponentDataValidationTests {

    @Test
    fun `Head of localizations map should always be the default locale`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        // A LinkedHashMap is ordered. Our defaultLocale is not the first nor the last item.
        val localizations = LinkedHashMap<LocaleId, Map<LocalizationKey, LocalizationData>>(3, 1f).apply {
            put(LocaleId("nl_NL"), mapOf(LocalizationKey("key") to LocalizationData.Text("waarde")))
            put(defaultLocale, mapOf(LocalizationKey("key") to LocalizationData.Text("value")))
            put(LocaleId("es_ES"), mapOf(LocalizationKey("key") to LocalizationData.Text("valor")))
        }
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = emptyList()),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        check(validated is PaywallValidationResult.Components)
        assertEquals(defaultLocale, validated.locales.head)
    }

    @Test
    fun `Should return AllLocalizationsMissing with Legacy fallback if all locales are missing`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = emptyList()),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            // We have no localizations.
            componentsLocalizations = emptyMap(),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        check(validated is PaywallValidationResult.Legacy)
        assertNotNull(validated.errors)
        assertEquals(validated.errors?.size, 1)
        assertEquals(validated.errors?.first(), AllLocalizationsMissing(defaultLocale))
    }

    @Test
    fun `Should accumulate errors with Legacy fallback if some localizations are missing`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("key1"),
                                color = ColorScheme(ColorInfo.Hex(Color.White.toArgb())),
                            ),
                            TextComponent(
                                text = LocalizationKey("key2"),
                                color = ColorScheme(ColorInfo.Hex(Color.White.toArgb())),
                            ),
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            // We're missing some localizations for nl_NL and es_ES.
            componentsLocalizations = mapOf(
                LocaleId("nl_NL") to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("waarde1"),
                ),
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                    LocalizationKey("key2") to LocalizationData.Text("value2"),
                ),
                LocaleId("es_ES") to mapOf(
                    LocalizationKey("key2") to LocalizationData.Text("valor2"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertTrue(validated is PaywallValidationResult.Legacy)
        assertNotNull(validated.errors)
        assertEquals(validated.errors!!.size, 2)
        assertTrue(
            validated.errors!!.contains(MissingStringLocalization(LocalizationKey("key2"), LocaleId("nl_NL")))
        )
        assertTrue(
            validated.errors!!.contains(MissingStringLocalization(LocalizationKey("key1"), LocaleId("es_ES")))
        )
    }

    @Test
    fun `Should not fail if missing localizations are not used`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        // There are no TextComponents using the localizations.
                        components = emptyList()
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            // We're missing some localizations for nl_NL and es_ES.
            componentsLocalizations = mapOf(
                LocaleId("nl_NL") to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("waarde1"),
                ),
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                    LocalizationKey("key2") to LocalizationData.Text("value2"),
                ),
                LocaleId("es_ES") to mapOf(
                    LocalizationKey("key2") to LocalizationData.Text("valor2"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertTrue(validated is PaywallValidationResult.Components)
        assertNull(validated.errors)
    }
}
