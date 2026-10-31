package com.revenuecat.e2etests.workflow

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallView

private const val WORKFLOW_OFFERING_ID = "default_workflows"

/**
 * Hosts the workflow paywall in a [PaywallView] inside an app-owned container, and re-targets that same
 * [PaywallView] to the exit offering when the paywall reports one. This mirrors the Unity SDK's Dialog-hosted
 * presenter and exercises [PaywallView.setExitOfferHandler].
 */
@Composable
fun PaywallViewExitOfferScreen(modifier: Modifier = Modifier) {
    var offering by remember { mutableStateOf<Offering?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var presenting by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            offering = Purchases.sharedInstance.awaitOfferings().getOffering(WORKFLOW_OFFERING_ID)
            if (offering == null) error = "Offering '$WORKFLOW_OFFERING_ID' not found"
        } catch (e: PurchasesException) {
            error = e.message ?: "Failed to load offerings"
        }
    }

    val loaded = offering
    if (presenting && loaded != null) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    showPaywall(
                        offeringId = loaded.identifier,
                        onDismiss = {
                            presenting = false
                            dismissed = true
                        },
                    )
                }
            },
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "PaywallView exit offer", style = MaterialTheme.typography.headlineMedium)
        if (dismissed) Text("Paywall dismissed")
        when {
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            loaded != null -> Button(onClick = { presenting = true }) { Text("Present Paywall") }
            else -> CircularProgressIndicator()
        }
    }
}

@OptIn(InternalRevenueCatAPI::class)
private fun FrameLayout.showPaywall(offeringId: String, onDismiss: () -> Unit) {
    val paywallView = PaywallView(context)
    paywallView.setOfferingId(offeringId)
    paywallView.setDismissHandler(onDismiss)
    paywallView.setExitOfferHandler { exitOffering ->
        // Deferred so the paywall isn't re-targeted from inside its own close callback.
        post {
            paywallView.setOfferingId(
                exitOffering.identifier,
                PresentedOfferingContext(exitOffering.identifier),
            )
        }
    }
    addView(
        paywallView,
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
    )
}
