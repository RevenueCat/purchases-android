package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Tracks in [unboundedState] whether this modifier's main-axis constraint leaves `weight` no space to
 * distribute. Place it last in a Row/Column chain to observe the constraint after the container's own
 * sizing/scroll, or before a leaf's `.size()` to observe the raw incoming one.
 *
 * That means max == Infinity *and* min == 0: Compose distributes `mainAxisMax`, or `mainAxisMin` when
 * max is Infinity (see RowColumnMeasurePolicy), so weight only collapses a child to zero when both
 * are gone. A scroll that keeps a non-zero min (the default root paywall scroll does) still leaves
 * weight working and must not count here. Callers skip `weight` when this is true, letting the child
 * take its natural size instead of collapsing.
 */
internal fun Modifier.trackMainAxisUnbounded(
    isHorizontal: Boolean,
    unboundedState: MutableState<Boolean>,
): Modifier = this.layout { measurable, constraints ->
    unboundedState.value = if (isHorizontal) {
        !constraints.hasBoundedWidth && constraints.minWidth == 0
    } else {
        !constraints.hasBoundedHeight && constraints.minHeight == 0
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
