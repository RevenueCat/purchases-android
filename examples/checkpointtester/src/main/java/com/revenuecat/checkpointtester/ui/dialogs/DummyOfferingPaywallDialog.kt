package com.revenuecat.checkpointtester.ui.dialogs

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.revenuecat.checkpointtester.checkpoints.DummyOfferingPaywallPresenter
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingCompletion
import kotlinx.coroutines.launch

/**
 * The dummy custom paywall behind [DummyOfferingPaywallPresenter]: the offering's packages as plain buy
 * buttons plus a dismiss button. A cancelled purchase keeps the dialog open; any other purchase error is
 * shown inline so the user can retry or dismiss.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
fun DummyOfferingPaywallDialog(
    request: DummyOfferingPaywallPresenter.Request,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var purchasing by remember(request) { mutableStateOf(false) }
    var errorMessage by remember(request) { mutableStateOf<String?>(null) }

    fun finish(report: (CheckpointOfferingCompletion) -> Unit) {
        report(request.completion)
        DummyOfferingPaywallPresenter.clear()
    }

    fun purchase(packageToPurchase: Package) {
        val purchasingActivity = activity ?: return
        purchasing = true
        errorMessage = null
        scope.launch {
            try {
                val result = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(purchasingActivity, packageToPurchase).build(),
                )
                finish { it.purchased(result.customerInfo, result.storeTransaction) }
            } catch (e: PurchasesException) {
                purchasing = false
                if (e.code != PurchasesErrorCode.PurchaseCancelledError) {
                    errorMessage = "Purchase failed: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!purchasing) finish { it.dismissed() } },
        modifier = modifier,
        title = { Text(text = "Dummy custom paywall") },
        text = {
            PaywallContent(
                request = request,
                buttonsEnabled = !purchasing && activity != null,
                errorMessage = errorMessage,
                onPurchase = ::purchase,
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { finish { it.dismissed() } }, enabled = !purchasing) {
                Text(text = "Dismiss")
            }
        },
    )
}

@Composable
private fun PaywallContent(
    request: DummyOfferingPaywallPresenter.Request,
    buttonsEnabled: Boolean,
    errorMessage: String?,
    onPurchase: (Package) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Presented by the app's CheckpointOfferingPresenter for offering " +
                "\"${request.offering.identifier}\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        request.offering.availablePackages.forEach { packageToPurchase ->
            Button(
                onClick = { onPurchase(packageToPurchase) },
                enabled = buttonsEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "${packageToPurchase.identifier}: ${packageToPurchase.product.price.formatted}")
            }
        }
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
