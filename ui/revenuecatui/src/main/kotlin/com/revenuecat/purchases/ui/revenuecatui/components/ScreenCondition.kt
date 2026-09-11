package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * Measures the size proposed to the paywall into [PaywallState.Loaded.Components.paywallBoundsDp]
 * for every state in [states], which window size rules evaluate against — a paywall in a sheet or
 * pane matches its own bounds, not the app window's, same as iOS. An unbounded axis (e.g. the
 * height of a fit-content sheet) falls back to the app window's dimension. Available synchronously
 * to [content], so rules resolve correctly on the first frame. Also reconciles the package
 * selection on bounds changes: a window size rule can hide the selected package (initial selection
 * happens before the bounds are known, and the bounds can change later).
 */
@JvmSynthetic
@Composable
internal fun MeasurePaywallBounds(
    states: List<PaywallState.Loaded.Components>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val bounds = if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
            DpSize(maxWidth, maxHeight)
        } else {
            val windowDpSize = currentWindowDpSize()
            DpSize(
                width = if (constraints.hasBoundedWidth) maxWidth else windowDpSize.width,
                height = if (constraints.hasBoundedHeight) maxHeight else windowDpSize.height,
            )
        }
        // Size-class overrides resolve against the app window, not the paywall bounds — the two
        // can fall in different size classes in a sheet or pane, so reconcile needs both.
        val screenCondition = ScreenCondition.from(currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass)
        for (state in states) {
            state.paywallBoundsDp = bounds
            state.windowScreenCondition = screenCondition
        }
        // Keyed on states too: workflow prewarming can add states while bounds stay constant,
        // and those join with a selection that was resolved before the bounds were known.
        LaunchedEffect(bounds, screenCondition, states) {
            for (state in states) {
                state.reconcileSelectionForWindowSize(bounds)
            }
        }
        content()
    }
}

@JvmSynthetic
@Composable
internal fun MeasurePaywallBounds(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = MeasurePaywallBounds(listOf(state), modifier, content)

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
