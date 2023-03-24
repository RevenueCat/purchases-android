package com.revenuecat.purchasetester

import com.revenuecat.purchases.models.SubscriptionOption

fun SubscriptionOption.toButtonString(isDefault: Boolean): String {
    val pricingPhasesString = pricingPhases.joinToString(separator = ",\n") { pricingPhase ->
        val formatted = pricingPhase.price.formatted
        val iso = pricingPhase.billingPeriod.iso8601
        val cycles = pricingPhase.billingCycleCount
        "\t[$formatted, $iso, $cycles cycles]"
    }
    return "${if (isDefault) "DEFAULT\n" else ""} ID: $id\n PricingPhases = [\n$pricingPhasesString\n],\nTags = $tags"
}
