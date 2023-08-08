package com.revenuecat.purchases.debugview.models

import com.revenuecat.purchases.Offering

internal sealed class SettingState(open val title: String) {
    data class Text(override val title: String, val content: String) : SettingState(title)
    class OfferingSetting(val offering: Offering) : SettingState(offering.identifier)
}
