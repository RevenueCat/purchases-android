package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig.AppConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.AllLocalizationsMissing
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.MissingStringLocalization
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class PaywallComponentDataValidationTests {

    private companion object {
        private val localizationKey = LocalizationKey("hello-world")

        const val EXPECTED_TEXT_EN = "Hello, world!"

        val paywallComponents = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = localizationKey,
                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
                            )
                        ),
                        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    localizationKey to LocalizationData.Text(EXPECTED_TEXT_EN),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), paywallComponents),
        )
    }


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

    @Test
    fun `Should successfully validate if all FontAliases are present`() {
        // Arrange
        val primaryFontAlias = FontAlias("primary")
        val secondaryFontAlias = FontAlias("secondary")
        val robotoFont = FontSpec.Resource(1)
        val robotoFontResourceName = FontInfo.Name("roboto")
        val openSansFont = FontSpec.Resource(2)
        val openSansFontResourceName = FontInfo.Name("open_sans")
        val uiConfig = UiConfig(
            app = AppConfig(
                fonts = mapOf(
                    primaryFontAlias to FontsConfig(robotoFontResourceName),
                    secondaryFontAlias to FontsConfig(openSansFontResourceName),
                ),
            ),
        )
        val resourceProvider = MockResourceProvider(
            mapOf(
                "font" to mapOf(
                    robotoFontResourceName.value to robotoFont.id,
                    openSansFontResourceName.value to openSansFont.id,
                ),
            ),
        )
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
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
                                color = textColor,
                                fontName = primaryFontAlias,
                            ),
                            TextComponent(
                                text = LocalizationKey("key2"),
                                color = textColor,
                                fontName = secondaryFontAlias,
                            ),
                        ),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                    LocalizationKey("key2") to LocalizationData.Text("value2"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(uiConfig, data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)

        // Assert
        val validatedComponents = validated as PaywallValidationResult.Components
        assertNull(validatedComponents.errors)
        val stack = validatedComponents.stack as StackComponentStyle
        assertEquals(2, stack.children.size)
        val text1 = stack.children[0] as TextComponentStyle
        val text2 = stack.children[1] as TextComponentStyle
        assertEquals(robotoFont, text1.fontSpec)
        assertEquals(openSansFont, text2.fontSpec)
    }

    @Test
    fun `Should accumulate errors if FontAliases are missing`() {
        // Arrange
        val missingFontAlias1 = FontAlias("missing-font-1")
        val missingFontAlias2 = FontAlias("missing-font-2")
        val missingFontAlias3 = FontAlias("missing-font-3")
        val existingFontAlias = FontAlias("primary")
        val existingFontResource = FontSpec.Resource(1)
        val existingFontResourceName = FontInfo.Name("roboto")
        val uiConfig = UiConfig(
            app = AppConfig(
                fonts = mapOf(
                    existingFontAlias to FontsConfig(existingFontResourceName),
                )
            ),
        )
        val resourceProvider = MockResourceProvider(
            resourceIds = mapOf("font" to mapOf(existingFontResourceName.value to existingFontResource.id)),
        )
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
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
                                color = textColor,
                                fontName = missingFontAlias1,
                                overrides = listOf(ComponentOverride(
                                    conditions = listOf(ComponentOverride.Condition.IntroOffer),
                                    properties = PartialTextComponent(fontName = missingFontAlias2),
                                ))
                            ),
                            TextComponent(
                                text = LocalizationKey("key2"),
                                color = textColor,
                                fontName = missingFontAlias3,
                            ),
                        ),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                    LocalizationKey("key2") to LocalizationData.Text("value2"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(uiConfig, data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)

        // Assert
        assertNotNull(validated.errors)
        assertEquals(3, validated.errors!!.size)
        assertTrue(validated.errors!!.contains(PaywallValidationError.MissingFontAlias(missingFontAlias1)))
        assertTrue(validated.errors!!.contains(PaywallValidationError.MissingFontAlias(missingFontAlias2)))
        assertTrue(validated.errors!!.contains(PaywallValidationError.MissingFontAlias(missingFontAlias3)))
    }

    // This tests a temporal hack to make the root component fill the screen. This will be removed once we have a
    // definite solution for positioning the root component.
    @Test
    fun `Should use Fill size in root component even if given Fit`() {
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        assert((validated.stack as StackComponentStyle).size.height == SizeConstraint.Fill)
    }

}
