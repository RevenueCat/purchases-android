package com.revenuecat.paywallstester.ui.screens.checkpoints

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowPresentationMode
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.launch

/**
 * The paywall this app draws for an offering when the checkpoints screen is set to present offerings itself, full
 * screen or as a bottom sheet over a scrim depending on the presentation mode the request asks for. Reports how the
 * user left it (purchased, closed through the X, continued without buying, or backed out through system back or,
 * for the sheet, a tap on the scrim); the SDK works out what the user obtained.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
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

    fun finish(result: PaywallPresenter.Completion.Result) {
        if (!busy) request.finish(result)
    }

    BackHandler(enabled = !busy) { finish(PaywallPresenter.Completion.Result.NavigatedBack) }

    val content: @Composable (fillHeight: Boolean) -> Unit = { fillHeight ->
        AppPaywallContent(
            request = request,
            fillHeight = fillHeight,
            selected = selected,
            busy = busy,
            message = message,
            canPurchase = selected != null && activity != null,
            onSelect = { selected = it },
            onPurchase = {
                val packageToPurchase = selected
                if (activity != null && packageToPurchase != null) {
                    checkout { PaywallCheckout.purchase(activity, packageToPurchase) }
                }
            },
            onRestore = { checkout { PaywallCheckout.restore() } },
            onFinish = ::finish,
        )
    }

    if (request.params.presentationMode == FlowPresentationMode.MODAL_FULL_SCREEN) {
        Surface(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) { content(true) }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    finish(PaywallPresenter.Completion.Result.NavigatedBack)
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) { content(false) }
            }
        }
    }
}

@OptIn(InviteOnlyCheckpointsAPI::class)
@Suppress("LongParameterList")
@Composable
private fun AppPaywallContent(
    request: AppPaywallPresenter.Request,
    fillHeight: Boolean,
    selected: Package?,
    busy: Boolean,
    message: String?,
    canPurchase: Boolean,
    onSelect: (Package) -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onFinish: (PaywallPresenter.Completion.Result) -> Unit,
) {
    Box {
        Column(
            modifier = Modifier
                .then(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppPaywallHeader(request.params)
            if (fillHeight) Spacer(modifier = Modifier.weight(1f))
            PackageList(
                packages = request.params.offering.availablePackages,
                selected = selected,
                enabled = !busy,
                onSelect = onSelect,
            )
            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (fillHeight) Spacer(modifier = Modifier.weight(1f))
            AppPaywallActions(
                busy = busy,
                canPurchase = canPurchase,
                onPurchase = onPurchase,
                onRestore = onRestore,
                onContinueWithoutBuying = { onFinish(PaywallPresenter.Completion.Result.Continued) },
            )
        }
        CloseButton(
            enabled = !busy,
            onClick = { onFinish(PaywallPresenter.Completion.Result.Closed) },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun CloseButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(Icons.Filled.Close, contentDescription = "Close")
    }
}

@OptIn(InviteOnlyCheckpointsAPI::class)
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
            text = "Drawn by the Paywall Tester app, not by RevenueCat, in mode ${params.presentationMode}. " +
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
