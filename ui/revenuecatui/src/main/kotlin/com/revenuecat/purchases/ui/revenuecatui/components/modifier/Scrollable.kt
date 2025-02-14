@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier

@JvmSynthetic
internal fun Modifier.scrollable(scrollState: ScrollState, scrollOrientation: Orientation): Modifier {
    val modifier = when (scrollOrientation) {
        Orientation.Vertical -> Modifier.verticalScroll(scrollState)
        Orientation.Horizontal -> Modifier.horizontalScroll(scrollState)
    }
    return this then modifier
}
