package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class StyleFactoryTests {

    private companion object {
        private val LOCALIZATION_KEY_TEXT_1 = LocalizationKey("text1")
        private val LOCALIZATION_KEY_TEXT_2 = LocalizationKey("text2")
    }

    private lateinit var styleFactory: StyleFactory

    private val localeId = LocaleId("en_US")
    private val localizations = nonEmptyMapOf(
        localeId to nonEmptyMapOf(
            LOCALIZATION_KEY_TEXT_1 to LocalizationData.Text("this is text 1"),
            LOCALIZATION_KEY_TEXT_2 to LocalizationData.Text("this is text 2"),
        )
    )
    private val colorAliases = emptyMap<ColorAlias, ColorScheme>()
    private val fontAliases = emptyMap<FontAlias, FontSpec>()
    private val variableLocalizations = nonEmptyMapOf(localeId to variableLocalizationKeysForEnUs())
    private val offering = Offering(
        identifier = "identifier",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = emptyList(),
    )

    @Before
    fun setup() {
        styleFactory = StyleFactory(
            localizations = localizations,
            colorAliases = colorAliases,
            fontAliases = fontAliases,
            variableLocalizations = variableLocalizations,
            offering = offering
        )
    }

    @Test
    fun `Should create a single TextComponentStyle for a single TextComponent`() {
        // Arrange
        val expectedColor = Color.Red
        val textComponent = TextComponent(
            text = LOCALIZATION_KEY_TEXT_1,
            color = ColorScheme(light = ColorInfo.Hex(expectedColor.toArgb())),
        )

        // Act
        val result = styleFactory.create(textComponent)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as TextComponentStyle
        assertThat(style.texts[localeId])
            .isEqualTo(localizations.getValue(localeId)[LOCALIZATION_KEY_TEXT_1]!!.value)
        val colorStyle = style.color.light as ColorStyle.Solid
        assertThat(colorStyle.color).isEqualTo(expectedColor)
    }

    @Test
    fun `Should create a StackComponentStyle with children for a StackComponent with children`() {
        // Arrange
        val stackComponent = StackComponent(
            components = listOf(
                TextComponent(
                    text = LOCALIZATION_KEY_TEXT_1,
                    color = ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb()))
                ),
                TextComponent(
                    text = LOCALIZATION_KEY_TEXT_2,
                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
                ),
            ),
            spacing = 8f
        )

        // Act
        val result = styleFactory.create(stackComponent)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as StackComponentStyle
        assertThat(style.spacing).isEqualTo(8.dp)
        assertThat(style.children).hasSize(2)
        with(style.children[0] as TextComponentStyle) {
            assertThat(texts[localeId]).isEqualTo(localizations.getValue(localeId)[LOCALIZATION_KEY_TEXT_1]!!.value)
        }
        with(style.children[1] as TextComponentStyle) {
            assertThat(texts[localeId]).isEqualTo(localizations.getValue(localeId)[LOCALIZATION_KEY_TEXT_2]!!.value)
        }
    }

    @Test
    fun `Should create a StackComponentStyle with backgroundColor if background not present`() {
        // Arrange
        val stackComponent = StackComponent(
            components = emptyList(),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            spacing = 8f
        )

        // Act
        val result = styleFactory.create(stackComponent)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as StackComponentStyle
        with(style.background as BackgroundStyles.Color) {
            val colorStyle = color.light as ColorStyle.Solid
            assertThat(colorStyle.color).isEqualTo(Color.Red)
        }
    }

    @Test
    fun `Should create a StackComponentStyle where background takes preference over backgroundColor`() {
        // Arrange
        val stackComponent = StackComponent(
            components = emptyList(),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
            spacing = 8f
        )

        // Act
        val result = styleFactory.create(stackComponent)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as StackComponentStyle
        with(style.background as BackgroundStyles.Color) {
            val colorStyle = color.light as ColorStyle.Solid
            assertThat(colorStyle.color).isEqualTo(Color.Blue)
        }
    }

    @Test
    fun `Should fail to create a TextComponentStyle if localized text is missing`() {
        // Arrange
        val otherLocale = LocaleId("nl_NL")
        val defaultLocale = LocaleId("en_US")
        val localizationKey = LocalizationKey("key")
        val otherLocalizationKey = LocalizationKey("other-key")
        val expectedText = "value"
        val unexpectedText = "waarde"
        val component = TextComponent(
            text = localizationKey,
            color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
        )
        val incorrectStyleFactory = StyleFactory(
            localizations = nonEmptyMapOf(
                defaultLocale to nonEmptyMapOf(
                    localizationKey to LocalizationData.Text(expectedText)
                ),
                otherLocale to nonEmptyMapOf(
                    otherLocalizationKey to LocalizationData.Text(unexpectedText)
                ),
            ),
            colorAliases = colorAliases,
            fontAliases = fontAliases,
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        // Act
        val result = incorrectStyleFactory.create(component)

        // Assert
        assertThat(result.isError).isTrue()
        val errors = result.errorOrNull()!!
        assertThat(errors.size).isEqualTo(1)
        val error = errors[0]
        assertThat(error).isInstanceOf(PaywallValidationError.MissingStringLocalization::class.java)
    }

    @Test
    fun `Should fail to create a TextComponentStyle if localized text is missing from an override`() {
        // Arrange
        val defaultLocale = LocaleId("en_US")
        val otherLocale = LocaleId("nl_NL")
        val baseLocalizationKey = LocalizationKey("key")
        val overrideLocalizationKey = LocalizationKey("override")
        val expectedText = "expected"
        val unexpectedText = "unexpected"
        val component = TextComponent(
            text = baseLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.IntroOffer),
                properties = PartialTextComponent(text = overrideLocalizationKey)
            ))
        )
        val incorrectStyleFactory = StyleFactory(
            localizations = nonEmptyMapOf(
                defaultLocale to nonEmptyMapOf(
                    baseLocalizationKey to LocalizationData.Text(unexpectedText),
                    overrideLocalizationKey to LocalizationData.Text(expectedText),
                ),
                otherLocale to nonEmptyMapOf(
                    baseLocalizationKey to LocalizationData.Text(unexpectedText),
                    // otherLocale is missing the overrideLocalizationKey.
                ),
            ),
            colorAliases = colorAliases,
            fontAliases = fontAliases,
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        // Act
        val result = incorrectStyleFactory.create(component)

        // Assert
        assertThat(result.isError).isTrue()
        val errors = result.errorOrNull()!!
        assertThat(errors.size).isEqualTo(1)
        val error = errors[0]
        assertThat(error).isInstanceOf(PaywallValidationError.MissingStringLocalization::class.java)
    }

    @Test
    fun `Should successfully create a TextComponentStyle with custom fonts`() {
        // Arrange
        val fontAliasBase = FontAlias("serif")
        val fontAliasOverride = FontAlias("monospace")
        val expectedBaseFontSpec = FontSpec.Generic.Serif
        val expectedOverrideFontSpec = FontSpec.Generic.Monospace
        val component = TextComponent(
            text = LOCALIZATION_KEY_TEXT_1,
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontName = fontAliasBase,
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.IntroOffer),
                properties = PartialTextComponent(fontName = fontAliasOverride)
            ))
        )
        val correctStyleFactory = StyleFactory(
            localizations = localizations,
            colorAliases = colorAliases,
            // Both fonts exist.
            fontAliases = mapOf(
                fontAliasBase to FontSpec.Generic.Serif,
                fontAliasOverride to FontSpec.Generic.Monospace,
            ),
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        // Act
        val result = correctStyleFactory.create(component)

        // Assert
        assertThat(result.isSuccess).isTrue()
        val textComponentStyle = result.getOrThrow() as TextComponentStyle
        val actualBaseFontSpec = textComponentStyle.fontSpec
        val actualOverrideFontSpec = textComponentStyle.overrides.firstOrNull()?.properties?.fontSpec

        assertThat(textComponentStyle.overrides).hasSize(1)
        assertThat(actualBaseFontSpec).isEqualTo(expectedBaseFontSpec)
        assertThat(actualOverrideFontSpec).isEqualTo(expectedOverrideFontSpec)
    }

    @Test
    fun `Should fail to create a TextComponentStyle if font is missing`() {
        // Arrange
        val expectedMissingFontAlias = FontAlias("does-not-exist")
        val component = TextComponent(
            text = LOCALIZATION_KEY_TEXT_1,
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            fontName = expectedMissingFontAlias
        )
        val incorrectStyleFactory = StyleFactory(
            localizations = localizations,
            colorAliases = colorAliases,
            // Empty on purpose
            fontAliases = emptyMap(),
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        // Act
        val result = incorrectStyleFactory.create(component)

        // Assert
        assertThat(result.isError).isTrue()
        val errors = result.errorOrNull()!!
        assertThat(errors.size).isEqualTo(1)
        val error = errors[0] as PaywallValidationError.MissingFontAlias
        assertThat(error.alias).isEqualTo(expectedMissingFontAlias)
    }

    @Test
    fun `Should fail to create a TextComponentStyle if font is missing from an override`() {
        // Arrange
        val expectedMissingFontAlias = FontAlias("does-not-exist")
        val component = TextComponent(
            text = LOCALIZATION_KEY_TEXT_1,
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.IntroOffer),
                properties = PartialTextComponent(fontName = expectedMissingFontAlias)
            ))
        )
        val incorrectStyleFactory = StyleFactory(
            localizations = localizations,
            colorAliases = colorAliases,
            // Empty on purpose
            fontAliases = emptyMap(),
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        // Act
        val result = incorrectStyleFactory.create(component)

        // Assert
        assertThat(result.isError).isTrue()
        val errors = result.errorOrNull()!!
        assertThat(errors.size).isEqualTo(1)
        val error = errors[0] as PaywallValidationError.MissingFontAlias
        assertThat(error.alias).isEqualTo(expectedMissingFontAlias)
    }

    @Test
    fun `Should fail to create a ButtonComponentStyle if localized URL is missing`() {
        // Arrange
        val otherLocale = LocaleId("nl_NL")
        val defaultLocale = LocaleId("en_US")
        val localizationKey = LocalizationKey("key")
        val otherLocalizationKey = LocalizationKey("other-key")
        val expectedText = "value"
        val unexpectedText = "waarde"
        val component = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(
                destination = ButtonComponent.Destination.Url(
                    urlLid = localizationKey,
                    method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                )
            ),
            stack = StackComponent(components = emptyList()),
        )
        val incorrectStyleFactory = StyleFactory(
            localizations = nonEmptyMapOf(
                defaultLocale to nonEmptyMapOf(
                    localizationKey to LocalizationData.Text(expectedText)
                ),
                otherLocale to nonEmptyMapOf(
                    otherLocalizationKey to LocalizationData.Text(unexpectedText)
                ),
            ),
            colorAliases = colorAliases,
            fontAliases = fontAliases,
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        // Act
        val result = incorrectStyleFactory.create(component)

        // Assert
        assertThat(result.isError).isTrue()
        val errors = result.errorOrNull()!!
        assertThat(errors.size).isEqualTo(1)
        val error = errors[0]
        assertThat(error).isInstanceOf(PaywallValidationError.MissingStringLocalization::class.java)
    }

    @Test
    fun `Should pair the default image with the default locale if there are no localized images`() {
        val defaultLocale = LocaleId("en_US")

        val expectedSources = (0..5).map { index ->
            ThemeImageUrls(
                light = ImageUrls(
                    original = URL("https://original$index"),
                    webp = URL("https://webp$index"),
                    webpLowRes = URL("https://webpLowRes$index"),
                    width = 100u,
                    height = 100u
                )
            )
        }
        val expectedBaseSource = expectedSources[0]
        val expectedIntroSource = expectedSources[1]
        val expectedSelectedSource = expectedSources[2]
        val expectedCompactSource = expectedSources[3]
        val expectedMediumSource = expectedSources[4]
        val expectedExpandedSource = expectedSources[5]

        val component = ImageComponent(
            source = expectedBaseSource,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.IntroOffer),
                    properties = PartialImageComponent(source = expectedIntroSource),
                ),
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Selected),
                    properties = PartialImageComponent(source = expectedSelectedSource),
                ),
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Compact),
                    properties = PartialImageComponent(source = expectedCompactSource),
                ),
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Medium),
                    properties = PartialImageComponent(source = expectedMediumSource),
                ),
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Expanded),
                    properties = PartialImageComponent(source = expectedExpandedSource),
                ),
            )
        )
        val styleFactory = StyleFactory(
            localizations = nonEmptyMapOf(
                // We have some localized text, but no images.
                defaultLocale to nonEmptyMapOf(
                    LocalizationKey("key-text") to LocalizationData.Text("value-text"),
                ),
            ),
            colorAliases = colorAliases,
            fontAliases = fontAliases,
            variableLocalizations = variableLocalizations,
            offering = offering,
        )

        val imageComponentStyle = styleFactory.create(component).getOrThrow() as ImageComponentStyle
        with(imageComponentStyle) {
            assertThat(sources.size).isEqualTo(1)
            assertThat(sources.getValue(defaultLocale)).isEqualTo(expectedBaseSource)
            assertThat(overrides).hasSize(5)
            assertThat(overrides[0].properties.sources?.size).isEqualTo(1)
            assertThat(overrides[0].properties.sources?.getValue(defaultLocale)).isEqualTo(expectedIntroSource)

            assertThat(overrides[1].properties.sources?.size).isEqualTo(1)
            assertThat(overrides[1].properties.sources?.getValue(defaultLocale)).isEqualTo(expectedSelectedSource)

            assertThat(overrides[2].properties.sources?.size).isEqualTo(1)
            assertThat(overrides[2].properties.sources?.getValue(defaultLocale))
                .isEqualTo(expectedCompactSource)

            assertThat(overrides[3].properties.sources?.size).isEqualTo(1)
            assertThat(overrides[3].properties.sources?.getValue(defaultLocale)).isEqualTo(expectedMediumSource)

            assertThat(overrides[4].properties.sources?.size).isEqualTo(1)
            assertThat(overrides[4].properties.sources?.getValue(defaultLocale))
                .isEqualTo(expectedExpandedSource)
        }
    }

    @Test
    fun `Should successfully create a buttons TabControlComponent inside a TabComponent`() {
        // Arrange
        // TabControlComponent is in a Tab, 2 levels deep.
        val component = TabsComponent(
            tabs = listOf(
                TabsComponent.Tab(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(TabControlComponent)
                            )
                        )
                    )
                ),
                TabsComponent.Tab(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(TabControlComponent)
                            )
                        )
                    )
                ),
            ),
            control = TabsComponent.TabControl.Buttons(
                stack = StackComponent(
                    components = listOf(
                        TabControlButtonComponent(
                            tabIndex = 0,
                            stack = StackComponent(
                                components = emptyList()
                            )
                        ),
                        TabControlButtonComponent(
                            tabIndex = 1,
                            stack = StackComponent(
                                components = emptyList()
                            )
                        ),
                    )
                )
            )
        )

        // Act
        val result = styleFactory.create(component)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as TabsComponentStyle
        assertThat(style.tabs.size).isEqualTo(2)
        assertThat(style.control).isInstanceOf(TabControlStyle.Buttons::class.java)
        repeat(2) { index ->
            val firstChildOfTab = style.tabs[index].stack.children[0] as StackComponentStyle
            assertThat(firstChildOfTab.children.size).isEqualTo(1)
            val tabControlInTab = firstChildOfTab.children[0]
            assertThat(tabControlInTab).isInstanceOf(TabControlStyle.Buttons::class.java)
            assertThat(style.control).isEqualTo(tabControlInTab)
        }
    }

    @Test
    fun `Should successfully create a toggle TabControlComponent inside a TabComponent`() {
        // Arrange
        // TabControlComponent is in a Tab, 2 levels deep.
        val component = TabsComponent(
            tabs = listOf(
                TabsComponent.Tab(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(TabControlComponent)
                            )
                        )
                    )
                ),
                TabsComponent.Tab(
                    stack = StackComponent(
                        components = listOf(
                            StackComponent(
                                components = listOf(TabControlComponent)
                            )
                        )
                    )
                ),
            ),
            control = TabsComponent.TabControl.Toggle(
                stack = StackComponent(
                    components = listOf(
                        TabControlToggleComponent(
                            defaultValue = true,
                            thumbColorOn = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            thumbColorOff = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            trackColorOn = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            trackColorOff = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                        ),
                    )
                )
            )
        )

        // Act
        val result = styleFactory.create(component)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as TabsComponentStyle
        assertThat(style.tabs.size).isEqualTo(2)
        assertThat(style.control).isInstanceOf(TabControlStyle.Toggle::class.java)
        repeat(2) { index ->
            val firstChildOfTab = style.tabs[index].stack.children[0] as StackComponentStyle
            assertThat(firstChildOfTab.children.size).isEqualTo(1)
            val tabControlInTab = firstChildOfTab.children[0]
            assertThat(tabControlInTab).isInstanceOf(TabControlStyle.Toggle::class.java)
            assertThat(style.control).isEqualTo(tabControlInTab)
        }
    }

    @Test
    fun `Should fail to create a TabControlComponent outside of a TabComponent`() {
        // Arrange
        // TabControlComponent has 2 Stack ancestors, but no Tab.
        val component = StackComponent(
            components = listOf(
                StackComponent(
                    components = listOf(TabControlComponent)
                )
            )
        )

        // Act
        val result = styleFactory.create(component)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val errors = (result as Result.Error).value
        assertThat(errors.size).isEqualTo(1)
        assertThat(errors[0]).isInstanceOf(PaywallValidationError.TabControlNotInTab::class.java)
    }

    @Test
    fun `Should fail to create a TabsComponent without tabs`() {
        // Arrange
        val component = TabsComponent(
            // Empty on purpose.
            tabs = emptyList(),
            control = TabsComponent.TabControl.Toggle(
                stack = StackComponent(
                    components = listOf(
                        TabControlToggleComponent(
                            defaultValue = true,
                            thumbColorOn = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            thumbColorOff = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            trackColorOn = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            trackColorOff = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                        ),
                    )
                )
            )
        )

        // Act
        val result = styleFactory.create(component)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val errors = (result as Result.Error).value
        assertThat(errors.size).isEqualTo(1)
        assertThat(errors[0]).isInstanceOf(PaywallValidationError.TabsComponentWithoutTabs::class.java)
    }
}
