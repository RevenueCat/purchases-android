package com.revenuecat.checkpointtester.ui.paywalls

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.revenuecat.checkpointtester.checkpoints.CheckoutOutcome
import com.revenuecat.checkpointtester.checkpoints.ParkedPaywallPresenter
import com.revenuecat.checkpointtester.checkpoints.PaywallCheckout
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.launch

/**
 * The paywall behind the per-call presenter: a bottom sheet over a scrim, one buy button per package, restore,
 * continue without buying, and "Not now". Reports how the user left it (purchased, closed through "Not now",
 * continued without buying, or backed out through system back or a tap on the scrim); the SDK works out what the
 * user obtained.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
fun LocalPaywall(
    request: ParkedPaywallPresenter.Request,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var busy by remember(request) { mutableStateOf(false) }
    var message by remember(request) { mutableStateOf<String?>(null) }

    fun checkout(action: suspend () -> CheckoutOutcome) {
        busy = true
        message = null
        scope.launch {
            when (val outcome = action()) {
                CheckoutOutcome.Completed -> request.finish(PaywallPresenter.Completion.Result.Purchased)
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
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            SheetContent(
                request = request,
                state = SheetUiState(busy, message, canPurchase = activity != null),
                onPurchase = { pkg -> activity?.let { checkout { PaywallCheckout.purchase(it, pkg) } } },
                onRestore = { checkout { PaywallCheckout.restore() } },
                onFinish = ::finish,
            )
        }
    }
}

private class SheetUiState(
    val busy: Boolean,
    val message: String?,
    val canPurchase: Boolean,
)

@OptIn(InternalRevenueCatAPI::class)
@Composable
private fun SheetContent(
    request: ParkedPaywallPresenter.Request,
    state: SheetUiState,
    onPurchase: (Package) -> Unit,
    onRestore: () -> Unit,
    onFinish: (PaywallPresenter.Completion.Result) -> Unit,
) {
    val busy = state.busy
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Local presenter", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Presented by the presenter passed in CheckpointParams for offering " +
                "\"${request.params.offering.identifier}\" (checkpoint \"${request.params.checkpointIdentifier}\").",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
        )
        val packages = request.params.offering.availablePackages
        if (packages.isEmpty()) {
            Text("This offering has no packages.", style = MaterialTheme.typography.bodyMedium)
        }
        packages.forEach { packageToPurchase ->
            Button(
                onClick = { onPurchase(packageToPurchase) },
                enabled = !busy && state.canPurchase,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (busy) "Please wait..." else "Unlock for ${packageToPurchase.product.price.formatted}",
                )
            }
        }
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onRestore, enabled = !busy) {
            Text("Restore")
        }
        TextButton(
            onClick = { onFinish(PaywallPresenter.Completion.Result.ContinuedWithoutPurchasing) },
            enabled = !busy,
        ) {
            Text("Continue without buying")
        }
        TextButton(onClick = { onFinish(PaywallPresenter.Completion.Result.Closed) }, enabled = !busy) {
            Text("Not now")
        }
    }
}
