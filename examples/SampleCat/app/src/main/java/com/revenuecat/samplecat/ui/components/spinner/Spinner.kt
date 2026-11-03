package com.revenuecat.samplecat.ui.components.spinner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.ui.theme.AccentColor
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A custom loading spinner that displays an animated yarn ball.
 * Matches the iOS SampleCat spinner design.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    tint: Color = AccentColor,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")

    // Ball rotation - continuous 360 degree rotation
    val ballRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ballRotation",
    )

    // Yarn oscillation - back and forth between -2 and +2 degrees
    val yarnAngle by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "yarnAngle",
    )

    val ballPainter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_yarn_ball))
    val yarnPainter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_yarn_string))

    Canvas(modifier = modifier.size(40.dp)) {
        // Draw yarn with oscillating rotation (pivot at trailing edge)
        rotate(
            degrees = yarnAngle,
            pivot = center.copy(x = size.width),
        ) {
            with(yarnPainter) {
                draw(
                    size = size,
                    colorFilter = ColorFilter.tint(tint),
                )
            }
        }

        // Draw ball with continuous rotation (pivot at center)
        rotate(
            degrees = ballRotation,
            pivot = center.copy(y = center.y * 1.02f), // Slight offset like iOS (0.51)
        ) {
            with(ballPainter) {
                draw(
                    size = size,
                    colorFilter = ColorFilter.tint(tint),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpinnerPreview() {
    SampleCatTheme {
        Spinner()
    }
}
