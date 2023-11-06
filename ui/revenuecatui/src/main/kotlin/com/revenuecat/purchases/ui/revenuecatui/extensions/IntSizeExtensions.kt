package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.unit.IntSize

val IntSize.aspectRatio: Float
    get() = width.toFloat() / height
