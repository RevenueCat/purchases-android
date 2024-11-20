package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ColorInfo {

    @Serializable
    @SerialName("hex")
    data class Hex(val value: String) : ColorInfo()

    @Serializable
    @SerialName("alias")
    data class Alias(val value: String) : ColorInfo()
}

@Serializable
internal data class ColorScheme(
    val light: ColorInfo,
    val dark: ColorInfo? = null,
)
