@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
internal fun rememberUpdatedStackComponentState(
    style: StackComponentStyle,
    paywallState: PaywallState.Loaded.Components,
    selected: Boolean,
): StackComponentState =
    rememberUpdatedStackComponentState(
        style = style,
        isEligibleForIntroOffer = paywallState.isEligibleForIntroOffer,
        selected = selected,
    )

@JvmSynthetic
@Composable
internal fun rememberUpdatedStackComponentState(
    style: StackComponentStyle,
    isEligibleForIntroOffer: Boolean = false,
    selected: Boolean = false,
): StackComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        StackComponentState(
            initialWindowSize = windowSize,
            initialIsEligibleForIntroOffer = isEligibleForIntroOffer,
            initialSelected = selected,
            style = style,
        )
    }.apply {
        update(
            windowSize = windowSize,
            isEligibleForIntroOffer = isEligibleForIntroOffer,
            selected = selected,
        )
    }
}

@Stable
internal class StackComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialIsEligibleForIntroOffer: Boolean,
    initialSelected: Boolean,
    private val style: StackComponentStyle,
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var isEligibleForIntroOffer by mutableStateOf(initialIsEligibleForIntroOffer)
    private var selected by mutableStateOf(initialSelected)
    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides?.buildPresentedPartial(windowCondition, isEligibleForIntroOffer, componentState)
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: true }

    @get:JvmSynthetic
    val children = style.children

    @get:JvmSynthetic
    val dimension by derivedStateOf { presentedPartial?.partial?.dimension ?: style.dimension }

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val spacing by derivedStateOf { presentedPartial?.partial?.spacing?.dp ?: style.spacing }

    @get:JvmSynthetic
    val backgroundColor by derivedStateOf { presentedPartial?.partial?.backgroundColor ?: style.backgroundColor }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val shape by derivedStateOf { presentedPartial?.partial?.shape?.toShape() ?: style.shape }

    @get:JvmSynthetic
    val border by derivedStateOf { presentedPartial?.partial?.border ?: style.border }

    @get:JvmSynthetic
    val shadow by derivedStateOf { presentedPartial?.partial?.shadow ?: style.shadow }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
        isEligibleForIntroOffer: Boolean? = null,
        selected: Boolean? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
        if (isEligibleForIntroOffer != null) this.isEligibleForIntroOffer = isEligibleForIntroOffer
        if (selected != null) this.selected = selected
    }
}
