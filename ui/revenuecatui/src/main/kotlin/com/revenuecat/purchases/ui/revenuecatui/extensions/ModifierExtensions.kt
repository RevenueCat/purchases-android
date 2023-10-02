package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

internal fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
