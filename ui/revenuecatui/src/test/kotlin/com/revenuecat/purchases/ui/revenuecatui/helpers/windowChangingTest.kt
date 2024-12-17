package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import kotlin.math.floor
import kotlin.math.min

/**
 * Test Composable behavior across window size changes without recreating the Activity. This scenario occurs if the
 * Activity has `screenSize|smallestScreenSize|orientation|screenLayout` defined in the `configChanges` attribute in
 * the manifest.
 *
 * @param arrange Return any setup data that needs to be built outside of the size-dependent content.
 * @param act Build the Composable content that is expected to react to window size changes here. Receives the return
 * value from [arrange].
 * @param assert Assert the content built in [act]. The provided [WindowSizeController] can be used to control the
 * current window size
 */
internal fun <T> ComposeContentTestRule.windowChangingTest(
    arrange: @Composable () -> T,
    act: @Composable (T) -> Unit,
    assert: ComposeTestRule.(WindowSizeController) -> Unit,
) {
    setContent {
        val baseConfiguration: Configuration = LocalConfiguration.current
        var windowSize by remember {
            mutableStateOf("${baseConfiguration.screenWidthDp}x${baseConfiguration.screenHeightDp}")
        }
        val size = remember {
            derivedStateOf {
                val (width, height) = windowSize.split('x')
                DpSize(width.toInt().dp, height.toInt().dp)
            }
        }

        val arrangeResult = arrange()
        DensityForcedSize(size) {
            Column {
                act(arrangeResult)
                TextField(
                    value = windowSize,
                    onValueChange = { windowSize = it },
                    modifier = Modifier.testTag("windowSize")
                )
            }
        }
    }

    assert(WindowSizeController(this))
}

internal class WindowSizeController(private val composeTestRule: ComposeTestRule) {
    /**
     * The resulting window size is somehow not exactly equal to the inputs here. Best to provide some margin.
     */
    fun setWindowSizeInexact(width: Dp, height: Dp) {
        composeTestRule
            .onNodeWithTag("windowSize")
            .performTextReplacement(text = "${width.value.toInt()}x${height.value.toInt()}")
    }
}

// Everything below is largely copied from:
// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-test/src/commonMain/kotlin/androidx/compose/ui/test/DensityForcedSize.kt

@Composable
private fun DensityForcedSize(
    sizeState: State<DpSize>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val size = sizeState.value

    SubcomposeLayout(
        modifier = modifier
    ) { constraints ->
        val measurables = subcompose(Unit) {
            val maxWidth = constraints.maxWidth.toDp()
            val maxHeight = constraints.maxHeight.toDp()
            val requiredWidth = if (size.isSpecified) {
                max(maxWidth, size.width)
            } else {
                maxWidth
            }
            val requiredHeight = if (size.isSpecified) {
                max(maxHeight, size.height)
            } else {
                maxHeight
            }
            // Compute the minimum density required so that both the requested width and height both
            // fit
            val density = LocalDensity.current.density * min(
                maxWidth / requiredWidth,
                maxHeight / requiredHeight,
            )
            val providedDensity = Density(
                // Override the density with the factor needed to meet both the minimum width and
                // height requirements, and the platform density requirements.
                density = coerceDensity(density),
                // Pass through the font scale
                fontScale = LocalDensity.current.fontScale
            )

            CompositionLocalProvider(
                LocalDensity provides providedDensity
            ) {
                Layout(
                    content = content,
                    // This size will now be guaranteed to be able to match the constraints
                    modifier = Modifier
                        .then(
                            if (size.isSpecified) {
                                Modifier.size(size)
                            } else {
                                Modifier
                            }
                        )
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeables.forEach {
                            it.placeRelative(0, 0)
                        }
                    }
                }
            }
        }

        val placeables = measurables.map { it.measure(constraints) }

        layout(placeables.maxOf(Placeable::width), placeables.maxOf(Placeable::height)) {
            placeables.forEach {
                it.placeRelative(0, 0)
            }
        }
    }
}

@Stable
private fun Modifier.size(size: DpSize) = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.constrain(
            Constraints.fixed(
                size.width.roundToPx(),
                size.height.roundToPx()
            )
        )
    )

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

private fun coerceDensity(density: Float): Float =
    floor(density * DisplayMetrics.DENSITY_DEFAULT) / DisplayMetrics.DENSITY_DEFAULT
