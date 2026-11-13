package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Tracks in [unboundedState] whether this modifier's main-axis maximum is infinite.
 * Place it last in a Row/Column chain to observe the constraint after the container's own
 * sizing/scroll, or before a leaf's `.size()` to observe the raw incoming one.
 *
 * Stacks include a non-zero minimum so Fill children can stretch to their viewport share and grow
 * past it. Leaves that only need a fallback when Fill would collapse use [requireZeroMinimum].
 */
internal fun Modifier.trackMainAxisUnbounded(
    isHorizontal: Boolean,
    unboundedState: MutableState<Boolean>,
    requireZeroMinimum: Boolean,
): Modifier = this.layout { measurable, constraints ->
    unboundedState.value = if (isHorizontal) {
        !constraints.hasBoundedWidth && (!requireZeroMinimum || constraints.minWidth == 0)
    } else {
        !constraints.hasBoundedHeight && (!requireZeroMinimum || constraints.minHeight == 0)
    }
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

internal fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

internal fun <T> Modifier.applyIfNotNull(value: T?, modifier: Modifier.(T) -> Modifier): Modifier {
    return if (value != null) {
        then(modifier(Modifier, value))
    } else {
        this
    }
}

internal fun <T, V> Modifier.applyIfNotNull(value: T?, value2: V?, modifier: Modifier.(T, V) -> Modifier): Modifier {
    return if (value != null && value2 != null) {
        then(modifier(Modifier, value, value2))
    } else {
        this
    }
}
