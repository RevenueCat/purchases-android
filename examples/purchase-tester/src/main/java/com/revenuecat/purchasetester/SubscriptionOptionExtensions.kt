package com.revenuecat.purchasetester

import com.revenuecat.purchases.models.SubscriptionOption

fun SubscriptionOption.toButtonString(): String {
    val pricingPhasesString = pricingPhases.joinToString(separator = ",\n") { pricingPhase ->
        "\t[${pricingPhase.formattedPrice}, ${pricingPhase.billingPeriod}, ${pricingPhase.billingCycleCount} cycles]"
    }
    return "PricingPhases = [\n$pricingPhasesString\n],\nTags = $tags"
}
