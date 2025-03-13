package com.revenuecat.purchases.ui.revenuecatui.components.button

import android.os.LocaleList
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import kotlinx.coroutines.CompletableDeferred
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `onClick ignores further clicks until processing current click is done`() {
        var actionHandleCalledCount = 0
        val completable = CompletableDeferred<Unit>()

        composeTestRule.setContent {
            val style = ButtonComponentStyle(
                stackComponentStyle = StackComponentStyle(
                    children = listOf(
                        TextComponentStyle(
                            texts = nonEmptyMapOf(LocaleId("en_US") to "Purchase"),
                            color = ColorStyles(
                                light = ColorStyle.Solid(Color.Black),
                            ),
                            fontSize = 15,
                            fontWeight = FontWeight.REGULAR.toFontWeight(),
                            fontSpec = null,
                            textAlign = CENTER.toTextAlign(),
                            horizontalAlignment = CENTER.toAlignment(),
                            backgroundColor = ColorStyles(
                                light = ColorStyle.Solid(Color.Yellow),
                            ),
                            visible = true,
                            size = Size(width = Fill, height = Fill),
                            padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                            margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0)
                                .toPaddingValues(),
                            rcPackage = null,
                            tabIndex = null,
                            variableLocalizations = nonEmptyMapOf(
                                LocaleId("en_US") to variableLocalizationKeysForEnUs()
                            ),
                            overrides = emptyList(),
                        ),
                    ),
                    dimension = Dimension.Vertical(alignment = CENTER, distribution = START),
                    visible = true,
                    size = Size(width = Fill, height = Fill),
                    spacing = 16.dp,
                    background = BackgroundStyles.Color(ColorStyles(ColorStyle.Solid(Color.Red))),
                    padding = PaddingValues(all = 16.dp),
                    margin = PaddingValues(all = 16.dp),
                    shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                    border = BorderStyles(width = 2.dp, colors = ColorStyles(ColorStyle.Solid(Color.Blue))),
                    shadow = ShadowStyles(
                        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                        radius = 10.dp,
                        x = 0.dp,
                        y = 3.dp,
                    ),
                    badge = null,
                    scrollOrientation = null,
                    rcPackage = null,
                    tabIndex = null,
                    overrides = emptyList(),
                ),
                action = ButtonComponentStyle.Action.PurchasePackage,
            )
            ButtonComponentView(
                style = style,
                state = FakePaywallState(),
                onClick = {
                    actionHandleCalledCount++
                    completable.await()
                }
            )
        }

        val purchaseButton = composeTestRule.onNodeWithText("Purchase")
        purchaseButton.assertExists()
        purchaseButton.performClick()
        purchaseButton.performClick()
        purchaseButton.performClick()

        assertThat(actionHandleCalledCount).isEqualTo(1)

        completable.complete(Unit)

        assertThat(actionHandleCalledCount).isEqualTo(1)

        purchaseButton.performClick()

        assertThat(actionHandleCalledCount).isEqualTo(2)
    }

    @Test
    fun `Should use the correct URL when the locale changes`(): Unit = with(composeTestRule) {
        val localeIdEnUs = LocaleId("en_US")
        val localeIdNlNl = LocaleId("nl_NL")
        val localizationKey = LocalizationKey("ineligible key")
        val expectedUrlEnUs = "expected"
        val expectedUrlNlNl = "verwacht"
        val component = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(
                destination = ButtonComponent.Destination.Url(
                    urlLid = localizationKey,
                    method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                )
            ),
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = localizationKey,
                        color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                    )
                )
            ),
        )
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                localizationKey to LocalizationData.Text(expectedUrlEnUs),
            ),
            localeIdNlNl to nonEmptyMapOf(
                localizationKey to LocalizationData.Text(expectedUrlNlNl),
            )
        )
        val styleFactory = StyleFactory(localizations = localizations)
        val style = styleFactory.create(component).getOrThrow().componentStyle as ButtonComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )

        // Act
        var clickedUrl: String? = null
        setContent {
            ButtonComponentView(
                style = style,
                onClick = { action ->
                    clickedUrl = action
                        .let { it as? PaywallAction.NavigateTo }
                        ?.let { it.destination as PaywallAction.NavigateTo.Destination.Url }
                        ?.url
                },
                state = state
            )
        }

        // Assert
        state.update(localeList = LocaleList(localeIdEnUs.toJavaLocale()))
        onNodeWithText(expectedUrlEnUs)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertThat(clickedUrl).isEqualTo(expectedUrlEnUs)

        state.update(localeList = LocaleList(localeIdNlNl.toJavaLocale()))
        onNodeWithText(expectedUrlNlNl)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertThat(clickedUrl).isEqualTo(expectedUrlNlNl)
    }

}
