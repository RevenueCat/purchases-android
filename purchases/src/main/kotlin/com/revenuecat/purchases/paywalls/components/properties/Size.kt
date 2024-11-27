package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class Size internal constructor(
    @get:JvmSynthetic val width: SizeConstraint,
    @get:JvmSynthetic val height: SizeConstraint,
)

@InternalRevenueCatAPI
@Serializable
sealed interface SizeConstraint {

    @Serializable
    @SerialName("fit")
    object Fit : SizeConstraint

    @Serializable
    @SerialName("fill")
    object Fill : SizeConstraint

    @Poko
    @Serializable
    @SerialName("fixed")
    class Fixed internal constructor(
        @get:JvmSynthetic val value: UInt,
    ) : SizeConstraint
}
