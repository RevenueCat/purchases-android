package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.Modifier

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
