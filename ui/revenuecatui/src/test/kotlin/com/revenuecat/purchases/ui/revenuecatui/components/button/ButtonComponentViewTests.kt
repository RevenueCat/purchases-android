package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
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
                            text = "Purchase",
                            color = ColorScheme(
                                light = ColorInfo.Hex(Color.Black.toArgb()),
                            ),
                            fontSize = FontSize.BODY_M,
                            fontWeight = FontWeight.REGULAR.toFontWeight(),
                            fontFamily = null,
                            textAlign = CENTER.toTextAlign(),
                            horizontalAlignment = CENTER.toAlignment(),
                            backgroundColor = ColorScheme(
                                light = ColorInfo.Hex(Color.Yellow.toArgb()),
                            ),
                            size = Size(width = Fill, height = Fill),
                            padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                            margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0)
                                .toPaddingValues(),
                            overrides = null,
                        ),
                    ),
                    dimension = Dimension.Vertical(alignment = CENTER, distribution = START),
                    size = Size(width = Fill, height = Fill),
                    spacing = 16.dp,
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Red.toArgb())),
                    padding = PaddingValues(all = 16.dp),
                    margin = PaddingValues(all = 16.dp),
                    shape = RoundedCornerShape(size = 20.dp),
                    border = Border(width = 2.0, color = ColorScheme(ColorInfo.Hex(Color.Blue.toArgb()))),
                    shadow = Shadow(
                        color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                        radius = 10.0,
                        x = 0.0,
                        y = 3.0
                    ),
                    overrides = null,
                ),
                action = PaywallAction.PurchasePackage,
                actionHandler = {
                    actionHandleCalledCount++
                    completable.await()
                }
            )
            ButtonComponentView(style = style, state = FakePaywallState())
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

}
