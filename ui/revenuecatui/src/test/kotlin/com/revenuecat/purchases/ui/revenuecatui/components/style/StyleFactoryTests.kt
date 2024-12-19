package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

    @Before
    fun setup() {
        styleFactory = StyleFactory(localizations)
    }

    @Test
    fun `Should create a single TextComponentStyle for a single TextComponent`() {
        // Arrange
        val expectedColorScheme = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))
        val textComponent = TextComponent(
            text = LOCALIZATION_KEY_TEXT_1,
            color = expectedColorScheme,
        )

        // Act
        val result = styleFactory.create(textComponent)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as TextComponentStyle
        assertThat(style.texts[localeId])
            .isEqualTo(localizations.getValue(localeId)[LOCALIZATION_KEY_TEXT_1]!!.value)
        assertThat(style.color).isEqualTo(expectedColorScheme)
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
            nonEmptyMapOf(
                defaultLocale to nonEmptyMapOf(
                    localizationKey to LocalizationData.Text(expectedText)
                ),
                otherLocale to nonEmptyMapOf(
                    otherLocalizationKey to LocalizationData.Text(unexpectedText)
                ),
            )
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
            overrides = ComponentOverrides(introOffer = PartialTextComponent(text = overrideLocalizationKey))
        )
        val incorrectStyleFactory = StyleFactory(
            nonEmptyMapOf(
                defaultLocale to nonEmptyMapOf(
                    baseLocalizationKey to LocalizationData.Text(unexpectedText),
                    overrideLocalizationKey to LocalizationData.Text(expectedText),
                ),
                otherLocale to nonEmptyMapOf(
                    baseLocalizationKey to LocalizationData.Text(unexpectedText),
                    // otherLocale is missing the overrideLocalizationKey. We should fall back to defaultLocale.
                ),
            )
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
}
