package com.revenuecat.e2etests.checkpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint

/**
 * Hits a checkpoint and reports what its callback received. The SDK presents whatever the checkpoint resolves to
 * (a flow, or its error dialog) over this activity; Maestro asserts on that and on the status line here.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun CheckpointScreen(
    identifier: String,
    modifier: Modifier = Modifier,
) {
    var status by remember { mutableStateOf("Callback: not invoked") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "Checkpoint", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Identifier: $identifier")
        Button(
            onClick = {
                Purchases.sharedInstance.checkpoint(identifier) { result -> status = result.describe() }
            },
        ) {
            Text("Hit checkpoint")
        }
        Text(text = status)
    }
}

@OptIn(InviteOnlyCheckpointsAPI::class)
private fun FlowResult?.describe(): String {
    if (this == null) return "Callback: passed with null"
    val entitlements = obtainedEntitlements.map { it.entitlementInfo.identifier }.sorted()
    return "Callback: passed, entitlements: ${entitlements.ifEmpty { listOf("none") }.joinToString()}"
}
