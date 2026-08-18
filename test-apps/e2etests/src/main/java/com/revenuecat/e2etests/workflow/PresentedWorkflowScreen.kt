package com.revenuecat.e2etests.workflow

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
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings

private const val WORKFLOW_OFFERING_ID = "default_workflows"

/**
 * Presents the workflow paywall via [com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher]
 * (the Activity-based, exit-offer-aware path) rather than the embedded [WorkflowScreen] composable, which does
 * not surface exit offers. [onPresentPaywall] hands the loaded offering back to the Activity's launcher.
 */
@Composable
fun PresentedWorkflowScreen(
    onPresentPaywall: (Offering) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offering by remember { mutableStateOf<Offering?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            offering = Purchases.sharedInstance.awaitOfferings().getOffering(WORKFLOW_OFFERING_ID)
            if (offering == null) error = "Offering '$WORKFLOW_OFFERING_ID' not found"
        } catch (e: PurchasesException) {
            error = e.message ?: "Failed to load offerings"
        }
    }

    val loaded = offering
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "Workflow paywall", style = MaterialTheme.typography.headlineMedium)
        when {
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            loaded != null -> Button(onClick = { onPresentPaywall(loaded) }) {
                Text("Present Paywall")
            }
            else -> CircularProgressIndicator()
        }
    }
}
