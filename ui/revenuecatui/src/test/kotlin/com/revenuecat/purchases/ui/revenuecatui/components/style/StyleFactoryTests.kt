package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StyleFactoryTests {

    private companion object {
        private val LOCALIZATION_KEY_TEXT_1 = LocalizationKey("text1")
        private val LOCALIZATION_KEY_TEXT_2 = LocalizationKey("text2")
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var styleFactory: StyleFactory

    private val localizationDictionary = mapOf(
        LOCALIZATION_KEY_TEXT_1 to LocalizationData.Text("this is text 1"),
        LOCALIZATION_KEY_TEXT_2 to LocalizationData.Text("this is text 2"),
    )

    @Before
    fun setup() {
        styleFactory = StyleFactory(localizationDictionary)
    }

    @Test
    fun `Should create a single TextComponentStyle for a single TextComponent`() = composeTestRule.setContent {
        // Arrange
        val expectedColorScheme = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))
        val textComponent = TextComponent(
            text = LOCALIZATION_KEY_TEXT_1,
            color = expectedColorScheme,
        )

        // Act
        val result = styleFactory.create(textComponent, {})

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val style = (result as Result.Success).value as TextComponentStyle
        assertThat(style.text).isEqualTo(localizationDictionary[LOCALIZATION_KEY_TEXT_1]!!.value)
        assertThat(style.color).isEqualTo(expectedColorScheme)
    }

    @Test
    fun `Should create a StackComponentStyle with children for a StackComponent with children`() =
        composeTestRule.setContent {
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
            val result = styleFactory.create(stackComponent, {})

            // Assert
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val style = (result as Result.Success).value as StackComponentStyle
            assertThat(style.spacing).isEqualTo(8.dp)
            assertThat(style.children).hasSize(2)
            with(style.children[0] as TextComponentStyle) {
                assertThat(text).isEqualTo(localizationDictionary[LOCALIZATION_KEY_TEXT_1]!!.value)
            }
            with(style.children[1] as TextComponentStyle) {
                assertThat(text).isEqualTo(localizationDictionary[LOCALIZATION_KEY_TEXT_2]!!.value)
            }
        }
}
