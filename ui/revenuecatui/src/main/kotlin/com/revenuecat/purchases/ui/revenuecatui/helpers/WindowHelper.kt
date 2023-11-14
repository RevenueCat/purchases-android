package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.WindowMetricsCalculator

@Composable
@ReadOnlyComposable
internal fun computeWindowWidthSizeClass(): WindowWidthSizeClass? {
    val activity = LocalActivity.current ?: return null
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = activity.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density).windowWidthSizeClass
}
