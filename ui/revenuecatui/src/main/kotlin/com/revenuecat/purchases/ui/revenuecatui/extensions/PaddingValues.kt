@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@JvmSynthetic
internal fun PaddingValues.calculateHorizontalPadding(layoutDirection: LayoutDirection): Dp =
    calculateStartPadding(layoutDirection) + calculateEndPadding(layoutDirection)

@JvmSynthetic
internal fun PaddingValues.calculateVerticalPadding(): Dp =
    calculateTopPadding() + calculateBottomPadding()
