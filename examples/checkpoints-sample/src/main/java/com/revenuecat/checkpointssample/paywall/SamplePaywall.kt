package com.revenuecat.checkpointssample.paywall

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.launch

/**
 * A simple full-screen paywall for the offering behind [request]: the packages as selectable cards, a purchase
 * button, restore, and a close button. Reports how the user left it; the SDK works out what they obtained.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
fun SamplePaywall(
    request: SamplePaywallPresenter.Request,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var selected by remember(request) { mutableStateOf(request.params.offering.availablePackages.firstOrNull()) }
    var busy by remember(request) { mutableStateOf(false) }
    var message by remember(request) { mutableStateOf<String?>(null) }

    fun finish(result: PaywallPresenter.Completion.Result) {
        request.completion.complete(result)
        SamplePaywallPresenter.clear()
    }

    fun purchase(packageToPurchase: Package) {
        val purchasingActivity = activity ?: return
        busy = true
        message = null
        scope.launch {
            try {
                Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(purchasingActivity, packageToPurchase).build(),
                )
                finish(PaywallPresenter.Completion.Result.Purchased)
            } catch (e: PurchasesException) {
                busy = false
                if (e.code != PurchasesErrorCode.PurchaseCancelledError) {
                    message = "Purchase failed: ${e.message}"
                }
            }
        }
    }

    fun restore() {
        busy = true
        message = null
        scope.launch {
            try {
                val customerInfo = Purchases.sharedInstance.awaitRestore()
                if (customerInfo.entitlements.active.isNotEmpty()) {
                    finish(PaywallPresenter.Completion.Result.Purchased)
                } else {
                    busy = false
                    message = "Nothing to restore."
                }
            } catch (e: PurchasesException) {
                busy = false
                message = "Restore failed: ${e.message}"
            }
        }
    }

    BackHandler(enabled = !busy) { finish(PaywallPresenter.Completion.Result.NavigatedBack) }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            IconButton(
                onClick = { finish(PaywallPresenter.Completion.Result.Closed) },
                enabled = !busy,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
            PaywallBody(
                request = request,
                state = PaywallUiState(selected, busy, message, canPurchase = activity != null),
                onSelect = { selected = it },
                onPurchase = ::purchase,
                onRestore = ::restore,
            )
        }
    }
}

private class PaywallUiState(
    val selected: Package?,
    val busy: Boolean,
    val message: String?,
    val canPurchase: Boolean,
)

@OptIn(InternalRevenueCatAPI::class)
@Composable
private fun PaywallBody(
    request: SamplePaywallPresenter.Request,
    state: PaywallUiState,
    onSelect: (Package) -> Unit,
    onPurchase: (Package) -> Unit,
    onRestore: () -> Unit,
) {
    val packages = request.params.offering.availablePackages
    val busy = state.busy
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Unlock the Tapper game", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Presented by the sample's own PaywallPresenter for offering " +
                "\"${request.params.offering.identifier}\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (packages.isEmpty()) {
            Text("This offering has no packages.", style = MaterialTheme.typography.bodyMedium)
        }
        packages.forEach { packageToPurchase ->
            PackageCard(
                packageToPurchase = packageToPurchase,
                selected = packageToPurchase == state.selected,
                enabled = !busy,
                onClick = { onSelect(packageToPurchase) },
            )
        }
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { state.selected?.let(onPurchase) },
            enabled = !busy && state.selected != null && state.canPurchase,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Please wait..." else "Continue")
        }
        TextButton(onClick = onRestore, enabled = !busy) {
            Text("Restore purchases")
        }
    }
}

@Composable
private fun PackageCard(
    packageToPurchase: Package,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(packageToPurchase.identifier, style = MaterialTheme.typography.titleMedium)
            Text(packageToPurchase.product.price.formatted, style = MaterialTheme.typography.titleMedium)
        }
    }
}
