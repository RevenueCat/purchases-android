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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint

/**
 * Hits a checkpoint and reports what its callback received. The SDK presents whatever the checkpoint resolves to
 * (a flow, or its error dialog) over this activity; Maestro asserts on that and on the status line here, which
 * counts the invocations so a flow can tell a callback that fired once from one that fired earlier too.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun CheckpointScreen(
    identifier: String,
    productIdentifier: String,
    modifier: Modifier = Modifier,
) {
    var status by remember { mutableStateOf("Callback: not invoked") }
    var details by remember { mutableStateOf(emptyList<String>()) }
    var invocations by remember { mutableIntStateOf(0) }

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
                Purchases.sharedInstance.checkpoint(identifier) { result ->
                    invocations++
                    status = "${result.describe()} (call #$invocations)"
                    details = result.details(productIdentifier)
                }
            },
        ) {
            Text("Hit checkpoint")
        }
        Text(text = status)
        details.forEach { Text(text = it) }
    }
}

@OptIn(InviteOnlyCheckpointsAPI::class)
private fun CheckpointResult.describe(): String {
    val flow = flow ?: return "Callback: passed, no flow"
    val entitlements = flow.obtainedEntitlements.map { it.entitlementInfo.identifier }.sorted()
    return "Callback: passed, entitlements: ${entitlements.ifEmpty { listOf("none") }.joinToString()}"
}

// One line per fact, without regex specials, so Maestro matches each literally.
@OptIn(InviteOnlyCheckpointsAPI::class)
private fun CheckpointResult.details(productIdentifier: String): List<String> {
    val products = flow?.obtainedProducts?.map { it.product.purchasingData.productId }?.sorted().orEmpty()
    val active = customer.entitlements.active.keys.sorted()
    return listOf(
        "Obtained products: ${products.ifEmpty { listOf("none") }.joinToString()}",
        "Entitlement $ENTITLEMENT_ID obtained: ${didObtainEntitlement(ENTITLEMENT_ID)}",
        "Product $productIdentifier obtained: ${didObtainProduct(productIdentifier)}",
        "Customer active entitlements: ${active.ifEmpty { listOf("none") }.joinToString()}",
    )
}

private const val ENTITLEMENT_ID = "pro"
