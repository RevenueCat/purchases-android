package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
sealed interface Background {
    @Serializable
    @SerialName("color")
    data class Color(@get:JvmSynthetic val value: ColorScheme) : Background

    @Serializable
    @SerialName("image")
    data class Image(@get:JvmSynthetic val value: ThemeImageUrls) : Background
}
