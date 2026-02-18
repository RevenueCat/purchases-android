package com.revenuecat.purchases.ui.revenuecatui

import android.content.res.AssetManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig.AppConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
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
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
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
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class PaywallComponentDataValidationTests {

    private companion object {
        private val localizationKey = LocalizationKey("hello-world")

        const val EXPECTED_TEXT_EN = "Hello, world!"

        val paywallComponents = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = localizationKey,
                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
                            ),
                            TestData.Components.monthlyPackageComponent,
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
            availablePackages = listOf(TestData.Packages.monthly),
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
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
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
            availablePackages = listOf(TestData.Packages.monthly),
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
            id = "paywall_id",
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
            availablePackages = listOf(TestData.Packages.monthly),
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
    fun `Should use language-only localizations if available`() {
        // Arrange
        data class Args(
            val defaultLocale: String,
            val localizationsLocale: String,
            val variableLocalizationsLocale: String,
        )

        listOf(
            Args(
                defaultLocale = "en_US",
                localizationsLocale = "en",
                variableLocalizationsLocale = "en",
            ),
            Args(
                defaultLocale = "zh_Hans",
                localizationsLocale = "zh_Hans",
                variableLocalizationsLocale = "zh_Hans",
            ),
            Args(
                defaultLocale = "zh_Hans_CN",
                localizationsLocale = "zh_Hans",
                variableLocalizationsLocale = "zh_Hans",
            ),
            Args(
                defaultLocale = "zh_CN",
                localizationsLocale = "zh_Hans_CN",
                variableLocalizationsLocale = "zh_Hans_CN",
            ),
        ).forEach { args ->
            val defaultLocale = LocaleId(args.defaultLocale)
            val localizations = mapOf(
                LocaleId(args.localizationsLocale) to mapOf(LocalizationKey("key") to LocalizationData.Text("value"))
            )
            val variableLocalizations = mapOf(
                LocaleId(args.variableLocalizationsLocale) to variableLocalizationKeysForEnUs()
            )
            val data = PaywallComponentsData(
                id = "paywall_id",
                templateName = "template",
                assetBaseURL = URL("https://assets.pawwalls.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
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
                availablePackages = listOf(TestData.Packages.monthly),
                paywallComponents = Offering.PaywallComponents(UiConfig(localizations = variableLocalizations), data),
            )

            // Act
            val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

            // Assert
            when(validated) {
                is PaywallValidationResult.Legacy ->
                    fail("Failed to validate $args, errors: " + validated.errors?.joinToString())
                is PaywallValidationResult.Components -> {
                    assertNull(validated.errors)
                    assertEquals(defaultLocale, validated.locales.head)
                }
            }
        }
    }

    @Test
    fun `Should accumulate errors with Legacy fallback if some localizations are missing`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
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
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        // There are no TextComponents using the localizations.
                        components = listOf(TestData.Components.monthlyPackageComponent)
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
            availablePackages = listOf(TestData.Packages.monthly),
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
        val tertiaryFontAlias = FontAlias("tertiary")
        val quaternaryFontAlias = FontAlias("quaternary")
        val robotoFont = FontSpec.Resource(
            fontFamily = FontFamily(
                listOf(
                    Font(resId = 1, weight = androidx.compose.ui.text.font.FontWeight(400), style = FontStyle.Normal),
                    Font(resId = 2, weight = androidx.compose.ui.text.font.FontWeight(700), style = FontStyle.Italic),
                )
            )
        )
        val mockAssetManager = mockk<AssetManager>()
        val openSansFont = FontSpec.Asset(
            fontFamily = FontFamily(
                listOf(
                    Font(
                        path = "fonts/open_sans_regular.ttf",
                        assetManager = mockAssetManager,
                        weight = androidx.compose.ui.text.font.FontWeight(400),
                        style = FontStyle.Normal
                    ),
                    Font(
                        path = "fonts/open_sans_bold.ttf",
                        assetManager = mockAssetManager,
                        weight = androidx.compose.ui.text.font.FontWeight(700),
                        style = FontStyle.Italic
                    )
                )
            )
        )
        val robotoFontRegularResourceName = FontInfo.Name(
            value = "roboto-regular",
            family = "roboto",
            weight = 400,
            style = com.revenuecat.purchases.paywalls.components.properties.FontStyle.NORMAL,
        )
        val robotoFontBoldItalicResourceName = FontInfo.Name(
            value = "roboto-bold",
            family = "roboto",
            weight = 700,
            style = com.revenuecat.purchases.paywalls.components.properties.FontStyle.ITALIC,
        )
        val openSansRegularFontAssetName = FontInfo.Name(
            value = "open_sans_regular",
            family = "open_sans",
            weight = 400,
            style = com.revenuecat.purchases.paywalls.components.properties.FontStyle.NORMAL,
        )
        val openSansBoldFontAssetName = FontInfo.Name(
            value = "open_sans_bold",
            family = "open_sans",
            weight = 700,
            style = com.revenuecat.purchases.paywalls.components.properties.FontStyle.ITALIC,
        )
        val uiConfig = UiConfig(
            app = AppConfig(
                fonts = mapOf(
                    primaryFontAlias to FontsConfig(robotoFontRegularResourceName),
                    secondaryFontAlias to FontsConfig(openSansRegularFontAssetName),
                    tertiaryFontAlias to FontsConfig(robotoFontBoldItalicResourceName),
                    quaternaryFontAlias to FontsConfig(openSansBoldFontAssetName),
                ),
            ),
        )
        val resourceProvider = MockResourceProvider(
            resourceIds = mapOf(
                "font" to mapOf(
                    robotoFontRegularResourceName.value to 1,
                    robotoFontBoldItalicResourceName.value to 2,
                ),
            ),
            assetPaths = listOf(
                "fonts/open_sans_regular.ttf",
                "fonts/open_sans_bold.ttf",
            ),
            mockAssetManager = mockAssetManager,
        )
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
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
                            TextComponent(
                                text = LocalizationKey("key3"),
                                color = textColor,
                                fontName = tertiaryFontAlias,
                            ),
                            TestData.Components.monthlyPackageComponent,
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
                    LocalizationKey("key3") to LocalizationData.Text("value3"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(uiConfig, data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)

        // Assert
        val validatedComponents = validated as PaywallValidationResult.Components
        assertNull(validatedComponents.errors)
        val stack = validatedComponents.stack as StackComponentStyle
        assertEquals(4, stack.children.size)
        val text1 = stack.children[0] as TextComponentStyle
        val text2 = stack.children[1] as TextComponentStyle
        val text3 = stack.children[2] as TextComponentStyle
        assertEquals(robotoFont, text1.fontSpec)
        assertEquals(openSansFont, text2.fontSpec)
        assertEquals(robotoFont, text3.fontSpec)
    }

    @Test
    fun `Should successfully validate with XML font family`() {
        // Arrange
        val primaryFontAlias = FontAlias("primary")
        val secondaryFontAlias = FontAlias("secondary")
        val xmlFontFamily = FontFamily(
            listOf(
                Font(resId = 2, weight = androidx.compose.ui.text.font.FontWeight(400), style = FontStyle.Normal),
                Font(resId = 3, weight = androidx.compose.ui.text.font.FontWeight(700), style = FontStyle.Italic),
            )
        )
        val robotoFont = FontSpec.Resource(fontFamily = xmlFontFamily)
        val robotoFontRegularResourceName = FontInfo.Name(
            value = "roboto",
            family = "roboto",
            weight = 400,
            style = com.revenuecat.purchases.paywalls.components.properties.FontStyle.NORMAL,
        )
        val robotoFontBoldItalicResourceName = FontInfo.Name(
            value = "roboto",
            family = "roboto",
            weight = 700,
            style = com.revenuecat.purchases.paywalls.components.properties.FontStyle.ITALIC,
        )
        val uiConfig = UiConfig(
            app = AppConfig(
                fonts = mapOf(
                    primaryFontAlias to FontsConfig(robotoFontRegularResourceName),
                    secondaryFontAlias to FontsConfig(robotoFontBoldItalicResourceName),
                ),
            ),
        )
        val resourceProvider = MockResourceProvider(
            resourceIds = mapOf(
                "font" to mapOf(
                    robotoFontRegularResourceName.value to 1,
                    robotoFontBoldItalicResourceName.value to 1,
                ),
            ),
            fontFamiliesByXmlResourceId = mapOf(1 to xmlFontFamily)
        )
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
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
                            TestData.Components.monthlyPackageComponent,
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
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(uiConfig, data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)

        // Assert
        val validatedComponents = validated as PaywallValidationResult.Components
        assertNull(validatedComponents.errors)
        val stack = validatedComponents.stack as StackComponentStyle
        assertEquals(3, stack.children.size)
        val text1 = stack.children[0] as TextComponentStyle
        val text2 = stack.children[1] as TextComponentStyle
        assertEquals(robotoFont, text1.fontSpec)
        assertEquals(robotoFont, text2.fontSpec)
    }

    @Test
    fun `Should ignore MissingFontAliases errors`() {
        // Arrange
        val missingFontAlias1 = FontAlias("missing-font-1")
        val missingFontAlias2 = FontAlias("missing-font-2")
        val missingFontAlias3 = FontAlias("missing-font-3")
        val existingFontAlias = FontAlias("primary")
        val existingFontResourceName = FontInfo.Name("roboto")
        val uiConfig = UiConfig(
            app = AppConfig(
                fonts = mapOf(
                    existingFontAlias to FontsConfig(existingFontResourceName),
                )
            ),
        )
        val resourceProvider = MockResourceProvider(
            resourceIds = mapOf("font" to mapOf(existingFontResourceName.value to 1)),
        )
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
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
                                overrides = listOf(
                                    ComponentOverride(
                                        conditions = listOf(ComponentOverride.Condition.IntroOffer()),
                                        properties = PartialTextComponent(fontName = missingFontAlias2),
                                    )
                                )
                            ),
                            TextComponent(
                                text = LocalizationKey("key2"),
                                color = textColor,
                                fontName = missingFontAlias3,
                            ),
                            TestData.Components.monthlyPackageComponent,
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
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(uiConfig, data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)

        // Assert
        assertNull(validated.errors)
    }

    @Test
    fun `Should not fail on a missing blank FontAlias`() {
        // Arrange
        val missingBlankFontAliasBase = FontAlias(" ")
        val missingBlankFontAliasOverride = FontAlias(" ")
        val uiConfig = UiConfig(
            app = AppConfig(
                // Our 2 font aliases are missing from the AppConfig.
                fonts = emptyMap(),
            ),
        )
        val resourceProvider = MockResourceProvider()
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("key1"),
                                color = textColor,
                                fontName = missingBlankFontAliasBase,
                                overrides = listOf(
                                    ComponentOverride(
                                        conditions = listOf(ComponentOverride.Condition.IntroOffer()),
                                        properties = PartialTextComponent(fontName = missingBlankFontAliasOverride),
                                    )
                                )
                            ),
                            TestData.Components.monthlyPackageComponent,
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
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(uiConfig, data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)

        // Assert
        assertNull(validated.errors)
        val validatedComponents = validated as PaywallValidationResult.Components
        val rootStack = validatedComponents.stack as StackComponentStyle
        val text = rootStack.children[0] as TextComponentStyle
        // We expect the font to be ignored if it was blank.
        assertNull(text.fontSpec)
        assertNotNull(text.overrides[0])
        assertNull(text.overrides[0].properties.fontSpec)
    }

    // This tests a temporal hack to make the root component fill the screen. This will be removed once we have a
    // definite solution for positioning the root component.
    @Test
    fun `Should use Fill size in root component even if given Fit`() {
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        assert((validated.stack as StackComponentStyle).size.height == SizeConstraint.Fill)
    }

    @Test
    fun `Should apply top window insets to the hero image parent`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                        components = listOf(
                            StackComponent(
                                dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
                                components = listOf(
                                    ImageComponent(
                                        source = ThemeImageUrls(
                                            light = ImageUrls(
                                                original = URL("https://preview"),
                                                webp = URL("https://preview"),
                                                webpLowRes = URL("https://preview"),
                                                width = 100u,
                                                height = 100u,
                                            ),
                                        )
                                    )
                                )
                            ),
                            TestData.Components.monthlyPackageComponent,
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertTrue(validated is PaywallValidationResult.Components)
        assertNull(validated.errors)
        val validatedComponents = validated as PaywallValidationResult.Components
        val actualStack = validatedComponents.stack as StackComponentStyle
        val heroParent = actualStack.children[0] as StackComponentStyle
        assertTrue(heroParent.applyTopWindowInsets)
        val heroImage = heroParent.children[0] as ImageComponentStyle
        assertTrue(heroImage.ignoreTopWindowInsets)
    }

    @Test
    fun `Should apply top window insets to the root if there is no hero image`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                        components = listOf(
                            StackComponent(
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, START),
                                components = listOf(
                                    StackComponent(components = listOf()),
                                    StackComponent(components = listOf()),
                                    StackComponent(components = listOf()),
                                )
                            ),
                            TestData.Components.monthlyPackageComponent,
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertTrue(validated is PaywallValidationResult.Components)
        assertNull(validated.errors)
        val validatedComponents = validated as PaywallValidationResult.Components
        val actualStack = validatedComponents.stack as StackComponentStyle
        assertTrue(actualStack.applyTopWindowInsets)
    }

    @Test
    fun `Should apply bottom window insets to the sticky footer`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                        components = listOf(
                            StackComponent(
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, START),
                                components = listOf(
                                    StackComponent(components = listOf()),
                                    StackComponent(components = listOf()),
                                    StackComponent(components = listOf()),
                                )
                            ),
                            TestData.Components.monthlyPackageComponent,
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = StickyFooterComponent(
                        stack = StackComponent(
                            dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                            components = listOf(
                                StackComponent(
                                    dimension = Dimension.Horizontal(VerticalAlignment.CENTER, START),
                                    components = listOf(
                                        StackComponent(components = listOf()),
                                        StackComponent(components = listOf()),
                                        StackComponent(components = listOf()),
                                    )
                                )
                            )
                        )
                    ),
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertNull(validated.errors)
        assertTrue(validated is PaywallValidationResult.Components)
        val validatedComponents = validated as PaywallValidationResult.Components
        val actualStickyFooter = validatedComponents.stickyFooter as StickyFooterComponentStyle?
        assertNotNull(actualStickyFooter)
        val actualStack = actualStickyFooter!!.stackComponentStyle
        assertTrue(actualStack.applyBottomWindowInsets)
    }

    @Test
    fun `Should apply bottom window insets to the root if there is no sticky footer`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                        components = listOf(
                            StackComponent(
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, START),
                                components = listOf(
                                    StackComponent(components = listOf()),
                                    StackComponent(components = listOf()),
                                    StackComponent(components = listOf()),
                                )
                            ),
                            TestData.Components.monthlyPackageComponent,
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertTrue(validated is PaywallValidationResult.Components)
        assertNull(validated.errors)
        val validatedComponents = validated as PaywallValidationResult.Components
        val actualStack = validatedComponents.stack as StackComponentStyle
        assertTrue(actualStack.applyBottomWindowInsets)
    }

    @Test
    fun `Should not fail to validate if no package component is found`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                        components = listOf(
                            TestData.Components.monthlyPackageComponent,
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.annual, TestData.Packages.lifetime),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertNull(validated.errors)
    }

    @Test
    fun `Should validate if at least one package component is found`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, START),
                        components = listOf(
                            TestData.Components.monthlyPackageComponent,
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                defaultLocale to mapOf(
                    LocalizationKey("key1") to LocalizationData.Text("value1"),
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.annual, TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )

        // Act
        val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, MockResourceProvider())

        // Assert
        assertNull(validated.errors)
    }
}
