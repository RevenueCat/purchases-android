package com.revenuecat.e2etests.checkpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedProduct
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint

/**
 * Hits a checkpoint and reports what its callback received. The SDK presents whatever the checkpoint resolves to
 * (a flow, or its error dialog) over this activity; Maestro asserts on that and on the status line here, which
 * counts the invocations so a flow can tell a callback that fired once from one that fired earlier too.
 *
 * With [useAppPresenter], an app-owned paywall presenter replaces the SDK's, so a flow can read which offering the
 * checkpoint resolved to. [attribute], when set, is the `should_use_custom_ui` attribute set before the hit.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun CheckpointScreen(
    identifier: String,
    modifier: Modifier = Modifier,
    useAppPresenter: Boolean = false,
    attribute: String? = null,
) {
    var status by remember { mutableStateOf("Callback: not invoked") }
    var details by remember { mutableStateOf(emptyList<String>()) }
    var invocations by remember { mutableIntStateOf(0) }
    var presenterInvocations by remember { mutableIntStateOf(0) }
    var presentedOffering by remember { mutableStateOf<String?>(null) }
    var presentedCompletion by remember { mutableStateOf<PaywallPresenter.Completion?>(null) }

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
                attribute?.let { Purchases.sharedInstance.setAttributes(mapOf(ATTRIBUTE_KEY to it)) }
                val params = if (useAppPresenter) {
                    appPresenterParams { offeringIdentifier, completion ->
                        presenterInvocations++
                        presentedOffering = offeringIdentifier
                        presentedCompletion = completion
                    }
                } else {
                    null
                }
                Purchases.sharedInstance.checkpoint(identifier, params) { result ->
                    invocations++
                    status = "${result.describe()} (call #$invocations)"
                    details = result.details()
                }
            },
        ) {
            Text("Hit checkpoint")
        }
        Text(text = status)
        details.forEach { Text(text = it) }
        if (useAppPresenter) {
            Text(text = "Presenter invocations: $presenterInvocations")
            Text(text = "Presented offering: ${presentedOffering ?: "none"}")
        }
    }

    presentedCompletion?.let { completion ->
        AppPresenterDialog(offeringIdentifier = presentedOffering) {
            presentedCompletion = null
            completion.complete(PaywallPresenter.Completion.Result.Closed)
        }
    }
}

@OptIn(InviteOnlyCheckpointsAPI::class)
private fun appPresenterParams(
    onPresent: (offeringIdentifier: String, completion: PaywallPresenter.Completion) -> Unit,
): CheckpointParams = CheckpointParams {
    paywallPresenter { params, completion -> onPresent(params.offering.identifier, completion) }
}

@Composable
private fun AppPresenterDialog(offeringIdentifier: String?, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "App presenter offering: $offeringIdentifier") },
        confirmButton = {
            Button(onClick = onClose) {
                Text("Close")
            }
        },
    )
}

@OptIn(InviteOnlyCheckpointsAPI::class)
private fun CheckpointResult.describe(): String {
    val flow = flow ?: return "Callback: passed, no flow"
    val entitlements = flow.obtainedEntitlements.map { it.entitlementInfo.identifier }.sorted()
    return "Callback: passed, entitlements: ${entitlements.ifEmpty { listOf("none") }.joinToString()}"
}

// One line per fact, without regex specials, so Maestro matches each literally.
@OptIn(InviteOnlyCheckpointsAPI::class)
private fun CheckpointResult.details(): List<String> {
    val products = flow?.obtainedProducts?.map { it.product.purchasingData.productId }?.sorted().orEmpty()
    val active = customer.entitlements.active.keys.sorted()
    return listOf(
        "Obtained products: ${products.ifEmpty { listOf("none") }.joinToString()}",
        "Entitlement $ENTITLEMENT_ID obtained: ${didObtainEntitlement(ENTITLEMENT_ID)}",
        "Product $PRODUCT_ID obtained: ${didObtainProduct(PRODUCT_ID)}",
        "Product $PRODUCT_ID offering: ${obtainedProduct(PRODUCT_ID)?.offeringIdentifier ?: "none"}",
        "Customer active entitlements: ${active.ifEmpty { listOf("none") }.joinToString()}",
    )
}

@OptIn(InviteOnlyCheckpointsAPI::class)
private val ObtainedProduct.offeringIdentifier: String?
    get() = product.presentedOfferingContext?.offeringIdentifier

// A custom subscriber attribute, not an SDK setting. The `[donottouch] Subs Attr. should_use_custom_ui true`
// audience of the `donottouch_sdk_*` checkpoint rules matches on it, so the name must stay the same.
private const val ATTRIBUTE_KEY = "should_use_custom_ui"

private const val ENTITLEMENT_ID = "pro"

// The checkpoint paywall's preselected package.
private const val PRODUCT_ID = "pro_monthly_subscription"
