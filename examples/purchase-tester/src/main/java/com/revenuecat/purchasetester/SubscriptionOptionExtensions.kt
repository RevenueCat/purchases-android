package com.revenuecat.purchasetester

import com.revenuecat.purchases.models.SubscriptionOption

fun SubscriptionOption.toButtonString(isDefault: Boolean): String {
    val pricingPhasesString = pricingPhases.joinToString(separator = ",\n") { pricingPhase ->
        "\t[${pricingPhase.formattedPrice}, ${pricingPhase.billingPeriod}, ${pricingPhase.billingCycleCount} cycles]"
    }
    return "${if (isDefault) "DEFAULT\n" else ""} PricingPhases = [\n$pricingPhasesString\n],\nTags = $tags"
}
