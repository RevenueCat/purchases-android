package com.revenuecat.paywallstester.ui.screens.checkpoints

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.launch

/**
 * The paywall this app draws for an offering when the checkpoints screen is set to present offerings itself.
 * Reports how the user left it (purchased, closed through the X, continued without buying, or backed out through
 * system back); the SDK works out what the user obtained.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
internal fun AppPaywall(
    request: AppPaywallPresenter.Request,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var selected by remember(request) { mutableStateOf(request.params.offering.availablePackages.firstOrNull()) }
    var busy by remember(request) { mutableStateOf(false) }
    var message by remember(request) { mutableStateOf<String?>(null) }

    fun checkout(action: suspend () -> CheckoutOutcome) {
        busy = true
        message = null
        scope.launch {
            when (val outcome = action()) {
                CheckoutOutcome.Completed -> request.finish(PaywallPresenter.Completion.Result.Continued)
                CheckoutOutcome.Cancelled -> busy = false
                is CheckoutOutcome.Failed -> {
                    busy = false
                    message = outcome.message
                }
            }
        }
    }

    BackHandler(enabled = !busy) { request.finish(PaywallPresenter.Completion.Result.NavigatedBack) }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppPaywallHeader(request.params)
                Spacer(modifier = Modifier.weight(1f))
                PackageList(
                    packages = request.params.offering.availablePackages,
                    selected = selected,
                    enabled = !busy,
                    onSelect = { selected = it },
                )
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AppPaywallActions(
                    busy = busy,
                    canPurchase = selected != null && activity != null,
                    onPurchase = {
                        val packageToPurchase = selected
                        if (activity != null && packageToPurchase != null) {
                            checkout { PaywallCheckout.purchase(activity, packageToPurchase) }
                        }
                    },
                    onRestore = { checkout { PaywallCheckout.restore() } },
                    onContinueWithoutBuying = {
                        request.finish(PaywallPresenter.Completion.Result.Continued)
                    },
                )
            }
            CloseButton(
                enabled = !busy,
                onClick = { request.finish(PaywallPresenter.Completion.Result.Closed) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun CloseButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(Icons.Filled.Close, contentDescription = "Close")
    }
}

@OptIn(InternalRevenueCatAPI::class)
@Composable
private fun AppPaywallHeader(params: PaywallPresenter.Params) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 32.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Text(
                text = "APP PAYWALL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Text(
            text = "Drawn by the Paywall Tester app, not by RevenueCat. " +
                "Offering \"${params.offering.identifier}\" for checkpoint \"${params.checkpointIdentifier}\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Unlock everything", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Choose a plan to continue.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PackageList(
    packages: List<Package>,
    selected: Package?,
    enabled: Boolean,
    onSelect: (Package) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (packages.isEmpty()) {
            Text(
                text = "This offering has no packages.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        packages.forEach { packageToPurchase ->
            PackageCard(
                packageToPurchase = packageToPurchase,
                selected = packageToPurchase == selected,
                enabled = enabled,
                onClick = { onSelect(packageToPurchase) },
            )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = packageToPurchase.product.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = packageToPurchase.packageType.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = packageToPurchase.product.price.formatted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AppPaywallActions(
    busy: Boolean,
    canPurchase: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onContinueWithoutBuying: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onPurchase,
            enabled = !busy && canPurchase,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = if (busy) "Please wait..." else "Continue")
        }
        TextButton(onClick = onRestore, enabled = !busy) {
            Text(text = "Restore purchases")
        }
        TextButton(onClick = onContinueWithoutBuying, enabled = !busy) {
            Text(text = "Continue without buying")
        }
    }
}
