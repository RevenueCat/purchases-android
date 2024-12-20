@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.SystemFontFamily
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    paywallState: PaywallState.Loaded.Components,
    selected: Boolean,
): TextComponentState =
    rememberUpdatedTextComponentState(
        style = style,
        localeProvider = { paywallState.locale },
        isEligibleForIntroOffer = paywallState.isEligibleForIntroOffer,
        selected = selected,
    )

@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    localeProvider: () -> Locale,
    isEligibleForIntroOffer: Boolean = false,
    selected: Boolean = false,
): TextComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        TextComponentState(
            initialWindowSize = windowSize,
            initialIsEligibleForIntroOffer = isEligibleForIntroOffer,
            initialSelected = selected,
            style = style,
            localeProvider = localeProvider,
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
internal class TextComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialIsEligibleForIntroOffer: Boolean,
    initialSelected: Boolean,
    private val style: TextComponentStyle,
    private val localeProvider: () -> Locale,
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
    val text by derivedStateOf {
        val localeId = localeProvider().toLocaleId()

        presentedPartial?.texts?.run { getOrDefault(localeId, entry.value) }
            ?: style.texts.run { getOrDefault(localeId, entry.value) }
    }

    @get:JvmSynthetic
    val color by derivedStateOf { presentedPartial?.partial?.color ?: style.color }

    @get:JvmSynthetic
    val fontSize by derivedStateOf { presentedPartial?.partial?.fontSize ?: style.fontSize }

    @get:JvmSynthetic
    val fontWeight by derivedStateOf { presentedPartial?.partial?.fontWeight?.toFontWeight() ?: style.fontWeight }

    @get:JvmSynthetic
    val fontFamily by derivedStateOf {
        presentedPartial?.partial?.fontName?.let { SystemFontFamily(it, fontWeight) } ?: style.fontFamily
    }

    @get:JvmSynthetic
    val textAlign by derivedStateOf {
        presentedPartial?.partial?.horizontalAlignment?.toTextAlign() ?: style.textAlign
    }

    @get:JvmSynthetic
    val horizontalAlignment by derivedStateOf {
        presentedPartial?.partial?.horizontalAlignment?.toAlignment() ?: style.horizontalAlignment
    }

    @get:JvmSynthetic
    val backgroundColor by derivedStateOf { presentedPartial?.partial?.backgroundColor ?: style.backgroundColor }

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

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
