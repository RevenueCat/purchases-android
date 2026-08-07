package com.revenuecat.sample.admob.ui.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.admob.loadAndTrackAd
import com.revenuecat.purchases.awaitCheckpoint
import com.revenuecat.purchases.checkpoints.CheckpointResult
import kotlinx.coroutines.launch

private const val BANNER_CHECKPOINT_ID = "banner_checkpoint"

/**
 * POC: hits the hardcoded "banner_checkpoint" identifier, which `RandomWorkflowCheckpointResolver`
 * resolves directly to `CheckpointResult.BannerAd` (no backend, no presentation/executor involved).
 * Renders a real AdMob banner inline using the ad unit ID the checkpoint resolution returned.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
internal fun CheckpointBannerAdContent() {
    var status by remember { mutableStateOf("Idle") }
    var bannerAdUnitId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Text(
        text = "Hits checkpoint '$BANNER_CHECKPOINT_ID' — resolves directly to a banner config, " +
            "no presentation/executor involved.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = "Status: $status",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                status = "Hitting checkpoint..."
                scope.launch {
                    status = try {
                        when (val result = Purchases.sharedInstance.awaitCheckpoint(BANNER_CHECKPOINT_ID)) {
                            is CheckpointResult.BannerAd -> {
                                bannerAdUnitId = result.adUnitId
                                "Resolved: adUnitId=${result.adUnitId}"
                            }
                            else -> "Unexpected result: $result"
                        }
                    } catch (e: PurchasesException) {
                        "Error: ${e.code}: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = status != "Hitting checkpoint...",
        ) {
            Text("Hit checkpoint")
        }

        bannerAdUnitId?.let { adUnitId ->
            CheckpointResolvedBanner(adUnitId)
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun CheckpointResolvedBanner(adUnitId: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAndTrackAd(adRequest = AdRequest.Builder().build(), placement = "checkpoint_banner")
            }
        },
    )
}
