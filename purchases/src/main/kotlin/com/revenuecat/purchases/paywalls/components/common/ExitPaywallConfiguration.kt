package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class ExitPaywallsConfiguration(
    val bounce: ExitPaywallConfiguration? = null,
    val abandonment: ExitPaywallConfiguration? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ExitPaywallConfiguration(
    @SerialName("offering_id")
    val offeringId: String,
    val presentation: ExitPaywallPresentation = ExitPaywallPresentation.SHEET,
    @SerialName("dismiss_current")
    val dismissCurrent: Boolean = false,
)

@InternalRevenueCatAPI
@Serializable
enum class ExitPaywallPresentation {
    @SerialName("sheet")
    SHEET,

    @SerialName("fullscreen")
    FULLSCREEN,
}
