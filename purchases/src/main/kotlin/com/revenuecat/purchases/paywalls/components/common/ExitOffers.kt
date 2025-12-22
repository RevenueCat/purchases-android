package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class ExitOffers(
    val dismiss: ExitOffer? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ExitOffer(
    @SerialName("offering_id")
    val offeringId: String,
)
