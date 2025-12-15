package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class ExitOffersConfiguration(
    val dismiss: ExitOfferConfiguration? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ExitOfferConfiguration(
    @SerialName("offering_id")
    val offeringId: String,
)
