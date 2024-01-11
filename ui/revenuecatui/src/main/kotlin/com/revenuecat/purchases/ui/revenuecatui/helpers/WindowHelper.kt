package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen

@Composable
@ReadOnlyComposable
internal fun computeWindowWidthSizeClass(): WindowWidthSizeClass {
    return windowSizeClass().windowWidthSizeClass
}

@Composable
@ReadOnlyComposable
internal fun computeWindowHeightSizeClass(): WindowHeightSizeClass {
    return windowSizeClass().windowHeightSizeClass
}

@Composable
@ReadOnlyComposable
internal fun PaywallState.Loaded.shouldUseLandscapeLayout(): Boolean {
    return shouldUseLandscapeLayout(mode = templateConfiguration.mode)
}

@Composable
@ReadOnlyComposable
private fun shouldUseLandscapeLayout(mode: PaywallMode): Boolean {
    return shouldUseLandscapeLayout(mode, computeWindowHeightSizeClass())
}

@VisibleForTesting
internal fun shouldUseLandscapeLayout(mode: PaywallMode, sizeClass: WindowHeightSizeClass): Boolean {
    return mode.isFullScreen && sizeClass == WindowHeightSizeClass.COMPACT
}

@ReadOnlyComposable
@Composable
private fun windowSizeClass(): WindowSizeClass {
    val (width, height) = getScreenSize()
    return WindowSizeClass.compute(width, height)
}

@ReadOnlyComposable
@Composable
private fun getScreenSize(): Pair<Float, Float> {
    return if (isInPreviewMode()) {
        // WindowMetricsCalculator returns (0, 0) on previews
        val configuration = LocalConfiguration.current
        configuration.screenWidthDp.toFloat() to configuration.screenHeightDp.toFloat()
    } else {
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)

        Pair(
            metrics.bounds.width() / density,
            metrics.bounds.height() / density,
        )
    }
}
