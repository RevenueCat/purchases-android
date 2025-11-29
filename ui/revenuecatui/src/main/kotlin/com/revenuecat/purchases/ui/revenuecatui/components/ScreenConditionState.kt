package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowHeightSizeClass
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
    private var windowHeightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.COMPACT

    var snapshot by mutableStateOf(ScreenConditionSnapshot())
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

    fun updateWindowSizeClasses(
        widthSizeClass: WindowWidthSizeClass,
        heightSizeClass: WindowHeightSizeClass,
    ) {
        windowWidthSizeClass = widthSizeClass
        windowHeightSizeClass = heightSizeClass
        recalculate()
    }

    fun updateScreenSizes(screenSizes: List<UiConfig.AppConfig.ScreenSize>?) {
        breakpoints = resolveBreakpoints(screenSizes)
        recalculate()
    }

    private fun recalculate() {
        val legacyCondition = ScreenCondition.from(windowWidthSizeClass)
        val currentWidth = layoutWidthDp
        val currentHeight = layoutHeightDp
        val orientation = when {
            currentWidth == null || currentHeight == null -> ScreenOrientation.UNKNOWN
            currentWidth > currentHeight -> ScreenOrientation.LANDSCAPE
            currentHeight > currentWidth -> ScreenOrientation.PORTRAIT
            else -> ScreenOrientation.PORTRAIT
        }

        val effectiveWidth = if (currentWidth == null || currentHeight == null) {
            null
        } else if (shouldUseLandscapeLayout(orientation)) {
            currentWidth
        } else {
            min(currentWidth, currentHeight)
        }

        val screenSize = effectiveWidth?.let { widthDp ->
            breakpoints.lastOrNull { it.width <= widthDp } ?: breakpoints.firstOrNull()
        }

        snapshot = ScreenConditionSnapshot(
            condition = legacyCondition,
            orientation = orientation,
            screenSize = screenSize,
        )
    }

    private fun shouldUseLandscapeLayout(orientation: ScreenOrientation): Boolean {
        if (windowHeightSizeClass == WindowHeightSizeClass.COMPACT) {
            return true
        }
        return orientation == ScreenOrientation.LANDSCAPE && windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
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
    val state = remember(screenSizes) {
        ScreenConditionState(screenSizes).also {
            it.updateScreenSizes(screenSizes)
        }
    }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    LaunchedEffect(adaptiveInfo.windowSizeClass) {
        state.updateWindowSizeClasses(
            widthSizeClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass,
            heightSizeClass = adaptiveInfo.windowSizeClass.windowHeightSizeClass,
        )
    }

    return state
}

internal fun Modifier.trackScreenCondition(
    state: ScreenConditionState,
    density: Density,
): Modifier = this.onSizeChanged { size ->
    val widthDp = size.width / density.density
    val heightDp = size.height / density.density
    state.updateLayoutSize(widthDp = widthDp, heightDp = heightDp)
}
