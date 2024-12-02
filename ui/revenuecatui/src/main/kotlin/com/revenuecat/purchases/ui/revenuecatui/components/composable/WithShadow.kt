@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.property.ShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.property.toShadowStyle

/**
 * Adds a [shadow] in the provided [shape] to the [content].
 */
@Composable
internal fun WithShadow(shadow: ShadowStyle, shape: Shape, content: @Composable () -> Unit) {
    // We cannot use the built-in .shadow() modifier, as it takes very different parameters from what we have in our
    // ShadowStyle. WithShadow also can't be a modifier itself, because the modifiers we use to draw the shadow
    // (offset, blur and background) cannot be part of the same modifier chain as those applied to the content. If they
    // are, they will be combined in ways we don't want.
    Layout(
        content = {
            // Content
            content()

            // Shadow
            Box(
                Modifier
                    .offset { IntOffset(x = shadow.x.roundToPx(), y = shadow.y.roundToPx()) }
                    .blur(shadow.radius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(shadow.color, shape),
            )
        },
    ) { measurables, constraints ->
        val contentMeasurable = measurables[0]
        val shadowMeasurable = measurables[1]

        // We measure the content first, then make the shadow exactly as big.
        val contentPlaceable = contentMeasurable.measure(constraints)
        val shadowPlaceable = shadowMeasurable.measure(
            constraints = Constraints(
                minWidth = contentPlaceable.measuredWidth,
                maxWidth = contentPlaceable.measuredWidth,
                minHeight = contentPlaceable.measuredHeight,
                maxHeight = contentPlaceable.measuredHeight,
            ),
        )

        layout(width = contentPlaceable.measuredWidth, height = contentPlaceable.measuredHeight) {
            // We place the shadow and the content on top of each other.
            shadowPlaceable.placeRelative(0, 0)
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

@Suppress("MagicNumber")
@Preview("Circle")
@Composable
private fun Shadow_Preview_Circle() {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        WithShadow(
            shadow = Shadow(
                color = ColorScheme(
                    light = ColorInfo.Hex(Color.Black.toArgb()),
                ),
                x = 0.0,
                y = 5.0,
                radius = 30.0,
            ).toShadowStyle(),
            shape = shape,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(100.dp)
                    .background(Color.Red, shape = shape),
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview("Square")
@Composable
private fun Shadow_Preview_Square() {
    val shape = RectangleShape
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        WithShadow(
            shadow = Shadow(
                color = ColorScheme(
                    light = ColorInfo.Hex(Color.Black.toArgb()),
                ),
                x = 10.0,
                y = 5.0,
                radius = 20.0,
            ).toShadowStyle(),
            shape = shape,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(100.dp)
                    .background(Color.Red, shape = shape),
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview("Gradient+CustomShape")
@Composable
private fun Shadow_Preview_Gradient_CustomShape() {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        WithShadow(
            shadow = Shadow(
                color = ColorScheme(
                    light = ColorInfo.Gradient.Linear(
                        degrees = 0f,
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.Red.toArgb(),
                                percent = 0.1f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.Green.toArgb(),
                                percent = 0.5f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.Blue.toArgb(),
                                percent = 0.9f,
                            ),
                        ),
                    ),
                ),
                x = 0.0,
                y = 5.0,
                radius = 10.0,
            ).toShadowStyle(),
            shape = shape,
        ) {
            Text(
                text = "GET UNLIMITED RGB",
                modifier = Modifier
                    .background(Color.Black, shape = shape)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
