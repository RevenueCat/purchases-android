package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Tracks in [unboundedState] whether this modifier's incoming main-axis constraint is unbounded (an
 * ancestor scrolls, or a `Fit` container sits under one that does). Place it last in a Row/Column
 * chain to observe the constraint after the container's own sizing/scroll, or before a leaf's
 * `.size()` to observe the raw incoming one.
 *
 * A `weight`-ed child under an unbounded main axis collapses to zero (Compose's weight distribution
 * falls back to the axis minimum), so callers skip `weight` in that case and let the child take its
 * natural size instead.
 */
internal fun Modifier.trackMainAxisUnbounded(
    isHorizontal: Boolean,
    unboundedState: MutableState<Boolean>,
): Modifier = this.layout { measurable, constraints ->
    unboundedState.value = if (isHorizontal) !constraints.hasBoundedWidth else !constraints.hasBoundedHeight
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
