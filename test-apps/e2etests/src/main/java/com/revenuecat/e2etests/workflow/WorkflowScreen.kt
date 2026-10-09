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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.launch

private const val WORKFLOW_OFFERING_ID = "default_workflows"
private const val ENTITLEMENT_ID = "pro"

@OptIn(InternalRevenueCatAPI::class)
private sealed interface OfferingState {
    data object Loading : OfferingState
    data class Loaded(val offering: Offering) : OfferingState
    data class LoadedWorkflow(
        val workflow: PublishedWorkflow,
        val offerings: Offerings,
        val uiConfig: UiConfig,
    ) : OfferingState
    data class Failed(val message: String) : OfferingState
}

@Composable
fun WorkflowScreen(
    modifier: Modifier = Modifier,
    usersCountOverride: Int? = null,
    offeringId: String? = null,
    logInAppUserIds: List<String> = emptyList(),
    workflowId: String? = null,
) {
    var offeringState by remember { mutableStateOf<OfferingState>(OfferingState.Loading) }
    var showPaywall by remember { mutableStateOf(false) }
    var customerInfo by remember { mutableStateOf<CustomerInfo?>(null) }
    var loggedInAppUserId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        customerInfo = loadCustomerInfo()
        offeringState = loadWorkflow(workflowId, offeringId)
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
    if (showPaywall && loaded is OfferingState.LoadedWorkflow) {
        InjectedWorkflowPaywall(
            loaded = loaded,
            usersCountOverride = usersCountOverride,
            onDismiss = { showPaywall = false },
        )
        return
    }

    WorkflowLauncher(
        offeringState = loaded,
        customerInfo = customerInfo,
        onPresentPaywall = { showPaywall = true },
        logInButtons = {
            LogInButtons(logInAppUserIds, loggedInAppUserId) { appUserId ->
                scope.launch {
                    customerInfo = try {
                        Purchases.sharedInstance.awaitLogIn(appUserId).customerInfo
                    } catch (@Suppress("SwallowedException") e: PurchasesException) {
                        null
                    }
                    offeringState = loadWorkflow(workflowId, offeringId)
                    loggedInAppUserId = appUserId
                }
            }
        },
        modifier = modifier,
    )
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
                ) = onDismiss()
            })
            .build(),
    )
}

/** Opens one workflow by id, bypassing the offering to workflow map. */
@OptIn(InternalRevenueCatAPI::class)
@Composable
private fun InjectedWorkflowPaywall(
    loaded: OfferingState.LoadedWorkflow,
    usersCountOverride: Int?,
    onDismiss: () -> Unit,
) {
    Paywall(
        options = PaywallOptions.Builder(dismissRequest = onDismiss)
            .injectedWorkflow(loaded.workflow, loaded.offerings, loaded.uiConfig)
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
                ) = onDismiss()
            })
            .build(),
    )
}

@Composable
private fun LogInButtons(
    appUserIds: List<String>,
    loggedInAppUserId: String?,
    onLogIn: (String) -> Unit,
) {
    appUserIds.forEach { appUserId ->
        Button(onClick = { onLogIn(appUserId) }) {
            Text("Log In as $appUserId")
        }
    }
    loggedInAppUserId?.let { Text("Logged in as $it") }
}

@Composable
private fun WorkflowLauncher(
    offeringState: OfferingState,
    customerInfo: CustomerInfo?,
    onPresentPaywall: () -> Unit,
    logInButtons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "Workflow paywall", style = MaterialTheme.typography.headlineMedium)

        when (offeringState) {
            is OfferingState.Loading -> CircularProgressIndicator()
            is OfferingState.Loaded,
            is OfferingState.LoadedWorkflow,
            -> Button(onClick = onPresentPaywall) {
                Text("Present Paywall")
            }
            is OfferingState.Failed -> Text(
                text = "Error: ${offeringState.message}",
                color = MaterialTheme.colorScheme.error,
            )
        }

        logInButtons()

        Text(text = "entitlement ($ENTITLEMENT_ID): ${entitlementStatus(customerInfo)}")
    }
}

/** The one load path, so logging in reloads whatever the flow opened with. */
private suspend fun loadWorkflow(workflowId: String?, offeringId: String?): OfferingState =
    if (workflowId != null) {
        loadWorkflowById(workflowId)
    } else {
        loadWorkflowOffering(offeringId ?: WORKFLOW_OFFERING_ID)
    }

private suspend fun loadCustomerInfo(): CustomerInfo? = try {
    Purchases.sharedInstance.awaitCustomerInfo()
} catch (@Suppress("SwallowedException") e: PurchasesException) {
    null
}

private suspend fun loadWorkflowOffering(offeringId: String): OfferingState = try {
    Purchases.sharedInstance.awaitOfferings().getOffering(offeringId)?.let(OfferingState::Loaded)
        ?: OfferingState.Failed("Offering '$offeringId' not found")
} catch (e: PurchasesException) {
    OfferingState.Failed(e.message ?: "Failed to load offerings")
}

@OptIn(InternalRevenueCatAPI::class)
private suspend fun loadWorkflowById(workflowId: String): OfferingState = try {
    OfferingState.LoadedWorkflow(
        workflow = Purchases.sharedInstance.awaitGetWorkflow(workflowId),
        offerings = Purchases.sharedInstance.awaitOfferings(),
        uiConfig = Purchases.sharedInstance.awaitGetUiConfig(),
    )
} catch (e: PurchasesException) {
    OfferingState.Failed(e.message ?: "Failed to load workflow '$workflowId'")
}

private fun entitlementStatus(customerInfo: CustomerInfo?): String {
    val entitlement = customerInfo?.entitlements?.all?.get(ENTITLEMENT_ID) ?: return "nil"
    return if (entitlement.isActive) "active" else "inactive"
}
