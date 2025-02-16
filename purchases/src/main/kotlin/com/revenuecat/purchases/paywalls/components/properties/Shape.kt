package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
sealed interface Shape {

    companion object {
        private val pillCornerRadiuses = CornerRadiuses.Percentage(all = 50)
    }

    @Serializable
    @Poko
    @SerialName("rectangle")
    class Rectangle(
        @get:JvmSynthetic
        val corners: CornerRadiuses? = null,
    ) : Shape

    @Serializable
    @SerialName("pill")
    object Pill : Shape

    val cornerRadiuses: CornerRadiuses
        get() = when (this) {
            is Rectangle -> corners ?: CornerRadiuses.Dp.zero
            else -> pillCornerRadiuses
        }
}
