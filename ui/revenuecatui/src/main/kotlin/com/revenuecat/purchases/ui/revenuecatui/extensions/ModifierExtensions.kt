package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Tracks, in [unboundedState], whether this modifier's incoming main-axis constraint is
 * unbounded (e.g. because an ancestor scrolls, or a `Fit`-sized container sits under one that
 * does). Where to place it in the chain depends on what the caller needs to observe: last, right
 * before a Row/Column, to see the constraint its own sizing (and any scroll) actually produces —
 * not the raw constraint it received from its parent, which a `Fixed` size or scroll modifier
 * further up this same chain may still turn bounded; or earlier, before a leaf's own `.size()`
 * call, to see the raw incoming constraint that sizing decision needs to react to.
 *
 * A container whose main axis is unbounded can't give a `weight`-ed child a meaningful share of
 * space (Compose's weight distribution falls back to the axis minimum, which is 0 whenever an
 * ancestor scroll or `Fit` wrapping relaxed it) — callers use this to skip `weight` in that case
 * and let the child fall back to its own natural size instead of collapsing to zero.
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
