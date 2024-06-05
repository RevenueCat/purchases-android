package com.revenuecat.purchasetester

import com.revenuecat.purchases.models.SubscriptionOption

fun SubscriptionOption.toButtonString(isDefault: Boolean): String {
    val pricingPhasesString = pricingPhases.joinToString(separator = ",\n") { pricingPhase ->
        val formatted = pricingPhase.price.formatted
        val iso = pricingPhase.billingPeriod.iso8601
        val cycles = pricingPhase.billingCycleCount
        "\t[$formatted, $iso, $cycles cycles]"
    }
    val installmentsString = installmentsInfo?.let { installmentsInfo ->
        val commitmentPaymentsCount = installmentsInfo.commitmentPaymentsCount
        val subsequentCommitmentPaymentsCount = installmentsInfo.subsequentCommitmentPaymentsCount
        "\nInstallmentsInfo = [" +
            "\n\tCommitment: $commitmentPaymentsCount," +
            "\n\tSubsequent: $subsequentCommitmentPaymentsCount" +
            "\n]"
    }
    return "${if (isDefault) "DEFAULT\n" else ""} " +
        "PricingPhases = [\n$pricingPhasesString\n],\n" +
        "Tags = $tags" +
        if (installmentsString != null) ",$installmentsString" else ""
}
