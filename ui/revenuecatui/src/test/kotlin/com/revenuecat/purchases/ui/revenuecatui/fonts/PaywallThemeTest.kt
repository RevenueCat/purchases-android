// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PaywallThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `theme override updates component colors images and legacy templates without changing device configuration`() {
        val deviceConfiguration = mutableStateOf(Configuration().apply {
            uiMode = Configuration.UI_MODE_TYPE_DESK or Configuration.UI_MODE_NIGHT_NO
            fontScale = 1.5f
            setLocale(Locale.FRANCE)
        })
        val override = mutableStateOf<Boolean?>(true)
        val colors = ColorStyles(ColorStyle.Solid(Color.Red), ColorStyle.Solid(Color.Blue))
        val images = ThemeImageUrls(light = mockk<ImageUrls>(), dark = mockk<ImageUrls>())
        val config = TestData.template1Offering.paywall!!.config
        val legacy = TemplateConfiguration(
            template = PaywallTemplate.TEMPLATE_1,
            mode = PaywallMode.FULL_SCREEN,
            packages = mockk(),
            configuration = config,
            images = TemplateConfiguration.Images(null, null, null),
            imagesByTier = emptyMap(),
            colors = config.colors,
            locale = Locale.US,
        )
        var selectedColor: ColorStyle? = null
        var selectedImage: ImageUrls? = null
        var legacyBackground: Color? = null
        var renderedConfiguration: Configuration? = null
        var outerDarkMode: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides deviceConfiguration.value) {
                val deviceDarkMode = isSystemInDarkTheme()
                SideEffect { outerDarkMode = deviceDarkMode }
                PaywallTheme(fontProvider = null, isDarkModeOverride = override.value) {
                    val color = colors.forCurrentTheme
                    val image = images.urlsForCurrentTheme
                    val background = legacy.getCurrentColors().background
                    val configuration = LocalConfiguration.current
                    SideEffect {
                        selectedColor = color
                        selectedImage = image
                        legacyBackground = background
                        renderedConfiguration = configuration
                    }
                }
            }
        }

        fun assertTheme(isDark: Boolean, deviceIsDark: Boolean) {
            composeTestRule.runOnIdle {
                assertThat(selectedColor).isEqualTo(if (isDark) colors.dark else colors.light)
                assertThat(selectedImage).isSameAs(if (isDark) images.dark else images.light)
                assertThat(legacyBackground).isEqualTo(if (isDark) Color.Black else Color.White)
                assertThat(outerDarkMode).isEqualTo(deviceIsDark)
                assertThat(renderedConfiguration!!.uiMode and Configuration.UI_MODE_TYPE_MASK)
                    .isEqualTo(Configuration.UI_MODE_TYPE_DESK)
                assertThat(renderedConfiguration!!.fontScale).isEqualTo(1.5f)
                assertThat(renderedConfiguration!!.locales[0]).isEqualTo(Locale.FRANCE)
            }
        }
        assertTheme(isDark = true, deviceIsDark = false)
        composeTestRule.runOnIdle { override.value = false }
        assertTheme(isDark = false, deviceIsDark = false)
        composeTestRule.runOnIdle {
            deviceConfiguration.value = Configuration(deviceConfiguration.value).apply {
                uiMode = Configuration.UI_MODE_TYPE_DESK or Configuration.UI_MODE_NIGHT_YES
            }
        }
        assertTheme(isDark = false, deviceIsDark = true)
        composeTestRule.runOnIdle { override.value = null }
        assertTheme(isDark = true, deviceIsDark = true)
    }
}
