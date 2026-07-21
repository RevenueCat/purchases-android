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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

private const val WORKFLOW_OFFERING_ID = "default_workflows"
private const val ENTITLEMENT_ID = "pro"

private sealed interface OfferingState {
    data object Loading : OfferingState
    data class Loaded(val offering: Offering) : OfferingState
    data class Failed(val message: String) : OfferingState
}

@Composable
fun WorkflowScreen(
    modifier: Modifier = Modifier,
    usersCountOverride: Int? = null,
) {
    var offeringState by remember { mutableStateOf<OfferingState>(OfferingState.Loading) }
    var showPaywall by remember { mutableStateOf(false) }
    var customerInfo by remember { mutableStateOf<CustomerInfo?>(null) }

    LaunchedEffect(Unit) {
        try {
            customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        } catch (@Suppress("SwallowedException") e: PurchasesException) {
            // Ignore; the entitlement surface shows "nil" until info arrives.
        }
        offeringState = try {
            val offering = Purchases.sharedInstance.awaitOfferings().getOffering(WORKFLOW_OFFERING_ID)
            if (offering != null) {
                OfferingState.Loaded(offering)
            } else {
                OfferingState.Failed("Offering '$WORKFLOW_OFFERING_ID' not found")
            }
        } catch (e: PurchasesException) {
            OfferingState.Failed(e.message ?: "Failed to load offerings")
        }
    }

    // Keep the entitlement surface live so it flips to "active" after a purchase.
    DisposableEffect(Unit) {
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
            customerInfo = it
        }
        onDispose { Purchases.sharedInstance.updatedCustomerInfoListener = null }
    }

    val loaded = offeringState
    if (showPaywall && loaded is OfferingState.Loaded) {
        WorkflowPaywall(
            offering = loaded.offering,
            usersCountOverride = usersCountOverride,
            onDismiss = { showPaywall = false },
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
        Text(text = "Workflow paywall", style = MaterialTheme.typography.headlineMedium)

        when (loaded) {
            is OfferingState.Loading -> CircularProgressIndicator()
            is OfferingState.Loaded -> Button(onClick = { showPaywall = true }) {
                Text("Present Paywall")
            }
            is OfferingState.Failed -> Text(
                text = "Error: ${loaded.message}",
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(text = "entitlement ($ENTITLEMENT_ID): ${entitlementStatus(customerInfo)}")
    }
}

@Composable
private fun WorkflowPaywall(
    offering: Offering,
    usersCountOverride: Int?,
    onDismiss: () -> Unit,
) {
    Paywall(
        options = PaywallOptions.Builder(dismissRequest = onDismiss)
            .setOffering(offering)
            .apply {
                if (usersCountOverride != null) {
                    setCustomVariables(
                        mapOf("users_count" to CustomVariableValue.Number(usersCountOverride.toDouble())),
                    )
                }
            }
            .setListener(object : PaywallListener {
                override fun onPurchaseCompleted(
                    customerInfo: CustomerInfo,
                    storeTransaction: StoreTransaction,
                ) {
                    onDismiss()
                }
            })
            .build(),
    )
}

private fun entitlementStatus(customerInfo: CustomerInfo?): String {
    val entitlement = customerInfo?.entitlements?.all?.get(ENTITLEMENT_ID) ?: return "nil"
    return if (entitlement.isActive) "active" else "inactive"
}
