@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.sample.admob.nextgen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.AdRewardConfiguration
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.awaitGetAdRewards

@Composable
internal fun ConfiguredRewardsCard(adUnitId: String) {
    var lookup by remember(adUnitId) { mutableIntStateOf(0) }
    var isLoading by remember(adUnitId) { mutableStateOf(true) }
    var rewards by remember(adUnitId) { mutableStateOf<ConfiguredAdRewards?>(null) }

    LaunchedEffect(adUnitId, lookup) {
        isLoading = true
        rewards = Purchases.sharedInstance.awaitGetAdRewards(adUnitId)
        isLoading = false
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Expected rewards", style = MaterialTheme.typography.titleSmall)
            Text(adUnitId, style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (isLoading) "Looking up rewards…" else rewards.description(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Configured rewards are for display. Earned rewards are reported separately.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { lookup++ }, enabled = !isLoading) {
                Text("Look up again")
            }
        }
    }
}

private fun ConfiguredAdRewards?.description(): String {
    val primary = this?.reward
    return when {
        this == null -> "Reward configuration unavailable. You can still load and show an ad."
        primary == null -> "No rewards configured for this ad unit."
        else -> buildList {
            add("Primary: ${primary.description()}")
            moreRewards.forEachIndexed { index, reward ->
                add("Additional ${index + 1}: ${reward.description()}")
            }
        }.joinToString("\n")
    }
}

private fun AdRewardConfiguration.description(): String = when (this) {
    is AdRewardConfiguration.VirtualCurrency -> "$amount $code"
    is AdRewardConfiguration.Entitlement -> "$identifier for $durationInSeconds seconds"
    else -> "Unsupported reward"
}
