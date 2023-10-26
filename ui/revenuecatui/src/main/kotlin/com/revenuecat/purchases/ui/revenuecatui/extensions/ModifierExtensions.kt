package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.Modifier

internal fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(this))
    } else {
        this
    }
}
