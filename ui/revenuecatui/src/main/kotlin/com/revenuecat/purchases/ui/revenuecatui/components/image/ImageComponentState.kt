@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.image

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
internal fun rememberUpdatedImageComponentState(
    style: ImageComponentStyle,
    paywallState: PaywallState.Loaded.Components,
    selected: Boolean,
): ImageComponentState =
    rememberUpdatedImageComponentState(
        style = style,
        localeProvider = { paywallState.locale },
        isEligibleForIntroOffer = paywallState.isEligibleForIntroOffer,
        selected = selected,
    )

@JvmSynthetic
@Composable
internal fun rememberUpdatedImageComponentState(
    style: ImageComponentStyle,
    localeProvider: () -> Locale,
    isEligibleForIntroOffer: Boolean = false,
    selected: Boolean = false,
): ImageComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val density = LocalDensity.current
    val darkMode = isSystemInDarkTheme()

    return remember(style) {
        ImageComponentState(
            initialWindowSize = windowSize,
            initialIsEligibleForIntroOffer = isEligibleForIntroOffer,
            initialSelected = selected,
            initialDensity = density,
            initialDarkMode = darkMode,
            style = style,
            localeProvider = localeProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
            isEligibleForIntroOffer = isEligibleForIntroOffer,
            selected = selected,
            density = density,
            darkMode = darkMode,
        )
    }
}

@Suppress("LongParameterList")
@Stable
internal class ImageComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialIsEligibleForIntroOffer: Boolean,
    initialSelected: Boolean,
    initialDensity: Density,
    initialDarkMode: Boolean,
    private val style: ImageComponentStyle,
    private val localeProvider: () -> Locale,
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var isEligibleForIntroOffer by mutableStateOf(initialIsEligibleForIntroOffer)
    private var selected by mutableStateOf(initialSelected)
    private var density by mutableStateOf(initialDensity)
    private var darkMode by mutableStateOf(initialDarkMode)
    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides?.buildPresentedPartial(windowCondition, isEligibleForIntroOffer, componentState)
    }
    private val themeImageUrls: ThemeImageUrls by derivedStateOf {
        val localeId = localeProvider().toLocaleId()

        presentedPartial?.sources?.run { getOrDefault(localeId, entry.value) }
            ?: style.sources.run { getOrDefault(localeId, entry.value) }
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: true }

    @get:JvmSynthetic
    val imageUrls: ImageUrls by derivedStateOf {
        if (darkMode) themeImageUrls.dark ?: themeImageUrls.light else themeImageUrls.light
    }

    @get:JvmSynthetic
    val size: Size by derivedStateOf {
        (presentedPartial?.partial?.size ?: style.size).adjustForImage(imageUrls, density)
    }

    @get:JvmSynthetic
    val shape: Shape? by derivedStateOf { presentedPartial?.partial?.maskShape?.toShape() ?: style.shape }

    @get:JvmSynthetic
    val overlay: ColorScheme? by derivedStateOf { presentedPartial?.partial?.colorOverlay ?: style.overlay }

    @get:JvmSynthetic
    val contentScale: ContentScale by derivedStateOf {
        presentedPartial?.partial?.fitMode?.toContentScale() ?: style.contentScale
    }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
        isEligibleForIntroOffer: Boolean? = null,
        selected: Boolean? = null,
        density: Density? = null,
        darkMode: Boolean? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
        if (isEligibleForIntroOffer != null) this.isEligibleForIntroOffer = isEligibleForIntroOffer
        if (selected != null) this.selected = selected
        if (density != null) this.density = density
        if (darkMode != null) this.darkMode = darkMode
    }

    /**
     * We don't want to have Fit in any dimension, as that resolves to zero, which results in an invisible image. So
     * instead, we use the px size from the provided [ImageUrls], converted to dp.
     */
    private fun Size.adjustForImage(imageUrls: ImageUrls, density: Density): Size =
        Size(
            width = when (width) {
                is Fit -> {
                    // If height is Fixed, we'll have to scale width by the same factor.
                    val scaleFactor = when (val height = height) {
                        is Fit,
                        is Fill,
                        -> 1f

                        is Fixed -> {
                            val imageHeightDp = with(density) { imageUrls.height.toInt().toDp() }
                            height.value.toFloat() / imageHeightDp.value
                        }
                    }
                    Fixed(with(density) { (scaleFactor * imageUrls.width.toInt()).toDp().value.toUInt() })
                }

                is Fill,
                is Fixed,
                -> width
            },
            height = when (height) {
                is Fit -> {
                    // If width is Fixed, we'll have to scale height by the same factor.
                    val scaleFactor = when (val width = width) {
                        is Fit,
                        is Fill,
                        -> 1f

                        is Fixed -> {
                            val imageWidthDp = with(density) { imageUrls.width.toInt().toDp() }
                            width.value.toFloat() / imageWidthDp.value
                        }
                    }

                    Fixed(with(density) { (scaleFactor * imageUrls.height.toInt()).toDp().value.toUInt() })
                }

                is Fill,
                is Fixed,
                -> height
            },
        )
}
