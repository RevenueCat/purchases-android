package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class FlexStackTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `horizontal flex stack excludes hidden children from distribution`() = with(composeTestRule) {
        val children = children(
            size = Size(width = Fixed(10u), height = Fixed(10u)),
        )

        setContent {
            HorizontalStack(
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.SPACE_BETWEEN,
                ),
                spacing = 10.dp,
                modifier = Modifier.requiredSize(width = 100.dp, height = 10.dp),
            ) {
                items(children) { index, child ->
                    ComponentView(
                        style = child,
                        state = previewEmptyState(),
                        onClick = {},
                        modifier = Modifier.testTag("child-$index"),
                    )
                }
            }
        }

        onNodeWithTag("child-0").assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("child-1").assertLeftPositionInRootIsEqualTo(90.dp)
        onNodeWithTag("child-2").assertDoesNotExist()
    }

    @Test
    fun `vertical flex stack excludes hidden children from distribution`() = with(composeTestRule) {
        val children = children(
            size = Size(width = Fixed(10u), height = Fixed(10u)),
        )

        setContent {
            VerticalStack(
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.CENTER,
                    distribution = FlexDistribution.SPACE_BETWEEN,
                ),
                spacing = 10.dp,
                modifier = Modifier.requiredSize(width = 10.dp, height = 100.dp),
            ) {
                items(children) { index, child ->
                    ComponentView(
                        style = child,
                        state = previewEmptyState(),
                        onClick = {},
                        modifier = Modifier.testTag("child-$index"),
                    )
                }
            }
        }

        onNodeWithTag("child-0").assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("child-1").assertTopPositionInRootIsEqualTo(90.dp)
        onNodeWithTag("child-2").assertDoesNotExist()
    }

    @Test
    fun `space around excludes a hidden middle child`() = with(composeTestRule) {
        val children = children(
            size = Size(width = Fixed(10u), height = Fixed(10u)),
            hiddenIndex = 1,
        )

        setContent {
            HorizontalStack(
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.SPACE_AROUND,
                ),
                spacing = 10.dp,
                modifier = Modifier.requiredSize(width = 100.dp, height = 10.dp),
            ) {
                items(children) { index, child ->
                    ComponentView(
                        style = child,
                        state = previewEmptyState(),
                        onClick = {},
                        modifier = Modifier.testTag("child-$index"),
                    )
                }
            }
        }

        onNodeWithTag("child-0").assertLeftPositionInRootIsEqualTo(18.dp)
        onNodeWithTag("child-1").assertDoesNotExist()
        onNodeWithTag("child-2").assertLeftPositionInRootIsEqualTo(73.dp)
    }

    @Test
    fun `space evenly excludes a hidden first child`() = with(composeTestRule) {
        val children = children(
            size = Size(width = Fixed(10u), height = Fixed(10u)),
            hiddenIndex = 0,
        )

        setContent {
            HorizontalStack(
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.SPACE_EVENLY,
                ),
                spacing = 10.dp,
                modifier = Modifier.requiredSize(width = 100.dp, height = 10.dp),
            ) {
                items(children) { index, child ->
                    ComponentView(
                        style = child,
                        state = previewEmptyState(),
                        onClick = {},
                        modifier = Modifier.testTag("child-$index"),
                    )
                }
            }
        }

        onNodeWithTag("child-0").assertDoesNotExist()
        onNodeWithTag("child-1").assertLeftPositionInRootIsEqualTo(23.dp)
        onNodeWithTag("child-2").assertLeftPositionInRootIsEqualTo(67.dp)
    }

    @Test
    fun `fill child receives remaining space before flex distribution`() = with(composeTestRule) {
        val children = listOf(
            previewStackComponentStyle(
                children = emptyList(),
                size = Size(width = Fixed(10u), height = Fixed(10u)),
            ),
            previewStackComponentStyle(
                children = emptyList(),
                size = Size(width = Fill(), height = Fixed(10u)),
            ),
        )

        setContent {
            HorizontalStack(
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.SPACE_BETWEEN,
                ),
                spacing = 10.dp,
                modifier = Modifier.requiredSize(width = 100.dp, height = 10.dp),
            ) {
                items(children) { index, child ->
                    ComponentView(
                        style = child,
                        state = previewEmptyState(),
                        onClick = {},
                        modifier = Modifier
                            .testTag("child-$index")
                            .then(if (index == 1) Modifier.weight(1f) else Modifier),
                    )
                }
            }
        }

        onNodeWithTag("child-0").assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("child-1")
            .assertLeftPositionInRootIsEqualTo(20.dp)
            .assertWidthIsEqualTo(80.dp)
    }

    private fun children(size: Size, hiddenIndex: Int = 2) = List(3) { index ->
        previewStackComponentStyle(
            children = emptyList(),
            visible = index != hiddenIndex,
            size = size,
        )
    }
}
