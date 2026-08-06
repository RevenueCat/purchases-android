package com.revenuecat.sample.admob.ui.ads

import android.app.Activity
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
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCheckpoint
import com.revenuecat.purchases.checkpoints.CheckpointResult
import kotlinx.coroutines.launch

private const val AD_CHECKPOINT_ID = "ad_checkpoint"

/**
 * POC: hits the hardcoded "ad_checkpoint" identifier, which
 * `RandomWorkflowCheckpointResolver` resolves to a mock ad-typed workflow (no backend). Proves
 * `AdCheckpointWorkflowExecutor` end to end: resolve -> present real AdMob interstitial via
 * [com.revenuecat.sample.admob.checkpoints.AdmobCheckpointPresenterImpl] -> report outcome.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
internal fun CheckpointAdContent(activity: Activity) {
    var status by remember { mutableStateOf("Idle") }
    val scope = rememberCoroutineScope()

    Text(
        text = "Hits checkpoint '$AD_CHECKPOINT_ID', mock-resolved to an ad workflow (no backend).",
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
                        when (val result = Purchases.sharedInstance.awaitCheckpoint(AD_CHECKPOINT_ID)) {
                            is CheckpointResult.AdPresented -> "Ad checkpoint finished: ${result.adOutcome}"
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
    }
}
