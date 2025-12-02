package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.UiConfig
import kotlin.math.min

internal class ScreenConditionState(
    screenSizes: List<UiConfig.AppConfig.ScreenSize>?,
) {
    private var breakpoints: List<UiConfig.AppConfig.ScreenSize> = resolveBreakpoints(screenSizes)
    private var layoutWidthDp: Float? = null
    private var layoutHeightDp: Float? = null
    private var windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT

    var snapshot by mutableStateOf(ScreenCondition())
        private set

    fun updateLayoutSize(widthDp: Float, heightDp: Float) {
        if (widthDp <= 0f || heightDp <= 0f) {
            layoutWidthDp = null
            layoutHeightDp = null
        } else {
            layoutWidthDp = widthDp
            layoutHeightDp = heightDp
        }
        recalculate()
    }

    fun updateWindowWidthSizeClass(widthSizeClass: WindowWidthSizeClass) {
        windowWidthSizeClass = widthSizeClass
        recalculate()
    }

    fun updateScreenSizes(screenSizes: List<UiConfig.AppConfig.ScreenSize>?) {
        breakpoints = resolveBreakpoints(screenSizes)
        recalculate()
    }

    private fun recalculate() {
        val currentWidth = layoutWidthDp
        val currentHeight = layoutHeightDp
        val orientation = when {
            currentWidth == null || currentHeight == null -> ScreenOrientation.UNKNOWN
            currentWidth > currentHeight -> ScreenOrientation.LANDSCAPE
            currentHeight > currentWidth -> ScreenOrientation.PORTRAIT
            else -> ScreenOrientation.PORTRAIT
        }

        // Always use the shorter dimension to determine screen size (device form factor).
        // This ensures a tablet stays "tablet" regardless of orientation.
        // The orientation condition handles landscape/portrait separately.
        val screenSize = if (currentWidth == null || currentHeight == null) {
            null
        } else {
            val shorterDimension = min(currentWidth, currentHeight)
            breakpoints.lastOrNull { it.width <= shorterDimension } ?: breakpoints.firstOrNull()
        }

        snapshot = ScreenCondition(
            orientation = orientation,
            screenSize = screenSize,
        )
    }

    private fun resolveBreakpoints(
        screenSizes: List<UiConfig.AppConfig.ScreenSize>?,
    ): List<UiConfig.AppConfig.ScreenSize> {
        val allSizes = screenSizes?.takeIf { it.isNotEmpty() } ?: UiConfig.AppConfig.ScreenSize.Defaults.all
        return allSizes.sortedBy { it.width }
    }
}

@Composable
internal fun rememberScreenConditionState(
    screenSizes: List<UiConfig.AppConfig.ScreenSize>?,
): ScreenConditionState {
    val adaptiveInfo = currentWindowAdaptiveInfo()

    return remember(screenSizes) {
        ScreenConditionState(screenSizes).also {
            it.updateScreenSizes(screenSizes)
        }
    }.apply {
        // Update synchronously to avoid race conditions with onSizeChanged.
        updateWindowWidthSizeClass(adaptiveInfo.windowSizeClass.windowWidthSizeClass)
    }
}

internal fun Modifier.trackScreenCondition(
    state: ScreenConditionState,
    density: Density,
): Modifier = this.onSizeChanged { size ->
    with(density) {
        state.updateLayoutSize(
            widthDp = size.width.toDp().value,
            heightDp = size.height.toDp().value,
        )
    }
}
