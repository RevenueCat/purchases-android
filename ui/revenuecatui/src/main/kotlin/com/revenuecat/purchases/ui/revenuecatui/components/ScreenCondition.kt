package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * The current app window size in dp. Reads [currentWindowSize]'s window metrics, so it
 * updates automatically on rotation, resize, and multi-window changes.
 */
@JvmSynthetic
@Composable
internal fun currentWindowDpSize(): DpSize = with(LocalDensity.current) {
    currentWindowSize().toSize().toDpSize()
}

/**
 * Measures the paywall's own bounds into [PaywallState.Loaded.Components.paywallBoundsDp], which
 * window size rules evaluate against — a paywall in a sheet or pane matches its own size, not the
 * app window's, same as iOS. An unbounded axis (e.g. the height of a fit-content sheet) falls back
 * to the app window's dimension. Available synchronously to [content], so rules resolve correctly
 * on the first frame.
 */
@JvmSynthetic
@Composable
internal fun MeasurePaywallBounds(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val windowDpSize = currentWindowDpSize()
    BoxWithConstraints(modifier) {
        state.paywallBoundsDp = DpSize(
            width = if (constraints.hasBoundedWidth) maxWidth else windowDpSize.width,
            height = if (constraints.hasBoundedHeight) maxHeight else windowDpSize.height,
        )
        content()
    }
}

internal enum class ScreenCondition {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    companion object {
        @JvmSynthetic
        fun from(sizeClass: WindowWidthSizeClass) =
            when (sizeClass) {
                WindowWidthSizeClass.COMPACT -> COMPACT
                WindowWidthSizeClass.MEDIUM -> MEDIUM
                WindowWidthSizeClass.EXPANDED -> EXPANDED
                else -> {
                    Logger.d("Unexpected WindowWidthSizeClass: '$sizeClass'. Falling back to COMPACT.")
                    COMPACT
                }
            }
    }
}
