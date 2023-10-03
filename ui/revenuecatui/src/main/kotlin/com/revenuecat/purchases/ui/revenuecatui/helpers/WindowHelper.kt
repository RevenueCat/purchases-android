package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.revenuecat.purchases.ui.revenuecatui.extensions.getActivity

@Composable
@ReadOnlyComposable
internal fun computeWindowWidthSizeClass(): WindowWidthSizeClass? {
    val activity = LocalContext.current.getActivity() ?: return null
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = LocalContext.current.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density).windowWidthSizeClass
}
