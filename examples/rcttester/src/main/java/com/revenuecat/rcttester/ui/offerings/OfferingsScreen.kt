@file:Suppress("TooManyFunctions")

package com.revenuecat.rcttester.ui.offerings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.rcttester.purchasing.PurchaseManager
import com.revenuecat.rcttester.purchasing.PurchaseOperationResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingsScreen(
    purchaseManager: PurchaseManager?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    var offerings by remember { mutableStateOf<Offerings?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedOfferingForMetadata by remember { mutableStateOf<Offering?>(null) }
    var selectedOfferingForPaywall by remember { mutableStateOf<Offering?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var purchasingPackageId by remember { mutableStateOf<String?>(null) }
    var purchaseResult by remember { mutableStateOf<PurchaseResult?>(null) }

    LaunchedEffect(refreshKey) {
        loadOfferings(
            onLoading = { isLoading = true },
            onError = { error = it },
            onSuccess = { offerings = it },
            onComplete = { isLoading = false },
        )
    }

    val onPurchasePackage: (Package) -> Unit = { packageItem ->
        purchasingPackageId = packageItem.identifier
        coroutineScope.launch {
            handlePurchase(activity, packageItem, purchaseManager) { result ->
                purchaseResult = result
            }
            purchasingPackageId = null
        }
    }

    OfferingsScreenContent(
        state = OfferingsScreenState(
            isLoading = isLoading,
            error = error,
            offerings = offerings,
            purchasingPackageId = purchasingPackageId,
            selectedOfferingForMetadata = selectedOfferingForMetadata,
            selectedOfferingForPaywall = selectedOfferingForPaywall,
            purchaseResult = purchaseResult,
        ),
        callbacks = OfferingsScreenCallbacks(
            onNavigateBack = onNavigateBack,
            onRetry = { refreshKey++ },
            onShowMetadata = { selectedOfferingForMetadata = it },
            onPresentPaywall = { selectedOfferingForPaywall = it },
            onDismissMetadata = { selectedOfferingForMetadata = null },
            onDismissPaywall = { selectedOfferingForPaywall = null },
            onDismissPurchaseResult = { purchaseResult = null },
            onPurchasePackage = onPurchasePackage,
        ),
        purchaseLogic = purchaseManager?.purchaseLogic,
        modifier = modifier,
    )
}

private data class OfferingsScreenState(
    val isLoading: Boolean,
    val error: String?,
    val offerings: Offerings?,
    val purchasingPackageId: String?,
    val selectedOfferingForMetadata: Offering?,
    val selectedOfferingForPaywall: Offering?,
    val purchaseResult: PurchaseResult?,
)

private data class OfferingsScreenCallbacks(
    val onNavigateBack: () -> Unit,
    val onRetry: () -> Unit,
    val onShowMetadata: (Offering) -> Unit,
    val onPresentPaywall: (Offering) -> Unit,
    val onDismissMetadata: () -> Unit,
    val onDismissPaywall: () -> Unit,
    val onDismissPurchaseResult: () -> Unit,
    val onPurchasePackage: (Package) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferingsScreenContent(
    state: OfferingsScreenState,
    callbacks: OfferingsScreenCallbacks,
    modifier: Modifier = Modifier,
    purchaseLogic: PurchaseLogic? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offerings") },
                navigationIcon = {
                    IconButton(onClick = callbacks.onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        OfferingsContent(
            state = OfferingsContentState(
                isLoading = state.isLoading,
                error = state.error,
                offerings = state.offerings,
                purchasingPackageId = state.purchasingPackageId,
            ),
            callbacks = OfferingsContentCallbacks(
                onRetry = callbacks.onRetry,
                onShowMetadata = callbacks.onShowMetadata,
                onPresentPaywall = callbacks.onPresentPaywall,
                onPurchasePackage = callbacks.onPurchasePackage,
            ),
            paddingValues = paddingValues,
        )
    }

    state.selectedOfferingForMetadata?.let { offering ->
        OfferingMetadataDialog(
            offering = offering,
            onDismiss = callbacks.onDismissMetadata,
        )
    }

    state.purchaseResult?.let { result ->
        PurchaseResultDialog(
            result = result,
            onDismiss = {
                callbacks.onDismissPurchaseResult()
                if (result is PurchaseResult.Success || result is PurchaseResult.SuccessCustomImplementation) {
                    callbacks.onNavigateBack()
                }
            },
        )
    }

    state.selectedOfferingForPaywall?.let { offering ->
        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setDismissRequest(callbacks.onDismissPaywall)
                .setOffering(offering)
                .setCustomPurchaseLogic(purchaseLogic)
                .build(),
        )
    }
}

private suspend fun loadOfferings(
    onLoading: () -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (Offerings) -> Unit,
    onComplete: () -> Unit,
) {
    onLoading()
    onError(null)
    if (!Purchases.isConfigured) {
        onError("SDK not configured")
        onComplete()
        return
    }
    try {
        onSuccess(Purchases.sharedInstance.awaitOfferings())
    } catch (e: PurchasesException) {
        onError(e.message)
    } finally {
        onComplete()
    }
}

private data class OfferingsContentState(
    val isLoading: Boolean,
    val error: String?,
    val offerings: Offerings?,
    val purchasingPackageId: String?,
)

private data class OfferingsContentCallbacks(
    val onRetry: () -> Unit,
    val onShowMetadata: (Offering) -> Unit,
    val onPresentPaywall: (Offering) -> Unit,
    val onPurchasePackage: (Package) -> Unit,
)

@Composable
private fun OfferingsContent(
    state: OfferingsContentState,
    callbacks: OfferingsContentCallbacks,
    paddingValues: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.error != null -> {
                ErrorView(
                    error = state.error,
                    onRetry = callbacks.onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.offerings != null -> {
                if (state.offerings.all.isEmpty()) {
                    EmptyOfferingsView(modifier = Modifier.align(Alignment.Center))
                } else {
                    OfferingsList(
                        offerings = state.offerings,
                        purchasingPackageId = state.purchasingPackageId,
                        onShowMetadata = callbacks.onShowMetadata,
                        onPresentPaywall = callbacks.onPresentPaywall,
                        onPurchasePackage = callbacks.onPurchasePackage,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Failed to load offerings",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyOfferingsView(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No Offerings",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No offerings are configured for this app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OfferingsList(
    offerings: Offerings,
    purchasingPackageId: String?,
    onShowMetadata: (Offering) -> Unit,
    onPresentPaywall: (Offering) -> Unit,
    onPurchasePackage: (Package) -> Unit,
) {
    val offeringsList = offerings.all.values.toList()
    val currentOffering = offerings.current
    val sortedOfferings = sortOfferings(offeringsList, currentOffering)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sortedOfferings) { offering ->
            OfferingCard(
                data = OfferingCardData(
                    offering = offering,
                    isCurrent = offering.identifier == currentOffering?.identifier,
                    purchasingPackageId = purchasingPackageId,
                ),
                callbacks = OfferingCardCallbacks(
                    onShowMetadata = { onShowMetadata(offering) },
                    onPresentPaywall = { onPresentPaywall(offering) },
                    onPurchasePackage = onPurchasePackage,
                ),
            )
        }
    }
}

private fun sortOfferings(
    offeringsList: List<Offering>,
    currentOffering: Offering?,
): List<Offering> {
    return if (
        currentOffering != null &&
        offeringsList.firstOrNull()?.identifier != currentOffering.identifier
    ) {
        listOf(currentOffering) +
            offeringsList.filter { it.identifier != currentOffering.identifier }
    } else {
        offeringsList
    }
}

private suspend fun handlePurchase(
    activity: Activity,
    packageItem: Package,
    purchaseManager: PurchaseManager?,
    onResult: (PurchaseResult) -> Unit,
) {
    if (purchaseManager == null) {
        onResult(PurchaseResult.Error("Purchase manager not initialized", null))
        return
    }
    val result = purchaseManager.purchase(activity, packageItem)
    onResult(
        when (result) {
            is PurchaseOperationResult.Success -> PurchaseResult.Success(
                customerInfo = result.customerInfo,
            )
            is PurchaseOperationResult.SuccessCustomImplementation -> PurchaseResult.SuccessCustomImplementation
            is PurchaseOperationResult.UserCancelled -> PurchaseResult.Cancelled
            is PurchaseOperationResult.Pending -> PurchaseResult.Error(
                "The purchase is pending approval (e.g., Ask to Buy). It may complete later.",
                null,
            )
            is PurchaseOperationResult.Failure -> PurchaseResult.Error(
                result.error,
                null,
            )
        },
    )
}

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("No activity found")
}

private sealed class PurchaseResult {
    data class Success(val customerInfo: CustomerInfo? = null) : PurchaseResult()
    /** Success from custom BillingClient flow; sample can show different context. */
    data object SuccessCustomImplementation : PurchaseResult()
    data object Cancelled : PurchaseResult()
    data class Error(val message: String, val code: PurchasesErrorCode?) : PurchaseResult()
}

private data class OfferingCardData(
    val offering: Offering,
    val isCurrent: Boolean,
    val purchasingPackageId: String?,
)

private data class OfferingCardCallbacks(
    val onShowMetadata: () -> Unit,
    val onPresentPaywall: () -> Unit,
    val onPurchasePackage: (Package) -> Unit,
)

@Composable
private fun OfferingCard(
    data: OfferingCardData,
    callbacks: OfferingCardCallbacks,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OfferingCardHeader(
                offering = data.offering,
                onShowMetadata = callbacks.onShowMetadata,
            )

            if (data.isCurrent) {
                CurrentOfferingLabel()
            }

            OfferingCardPackages(
                packages = data.offering.availablePackages,
                purchasingPackageId = data.purchasingPackageId,
                onPurchasePackage = callbacks.onPurchasePackage,
            )

            if (data.offering.hasPaywall) {
                PresentPaywallButton(onClick = callbacks.onPresentPaywall)
            }
        }
    }
}

@Composable
private fun OfferingCardHeader(
    offering: Offering,
    onShowMetadata: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = offering.identifier,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (offering.serverDescription.isNotEmpty()) {
                Text(
                    text = offering.serverDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onShowMetadata) {
            Icon(Icons.Default.Info, contentDescription = "Show metadata")
        }
    }
}

@Composable
private fun CurrentOfferingLabel() {
    Text(
        text = "CURRENT",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun OfferingCardPackages(
    packages: List<Package>,
    purchasingPackageId: String?,
    onPurchasePackage: (Package) -> Unit,
) {
    if (packages.isNotEmpty()) {
        Text(
            text = "Packages:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        packages.forEach { packageItem ->
            PackageRow(
                packageItem = packageItem,
                isPurchasing = purchasingPackageId == packageItem.identifier,
                onPurchase = { onPurchasePackage(packageItem) },
            )
        }
    } else {
        Text(
            text = "No packages available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PackageRow(
    packageItem: Package,
    isPurchasing: Boolean,
    onPurchase: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = packageItem.product.title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = packageItem.identifier,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onPurchase,
            enabled = !isPurchasing,
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(packageItem.product.price.formatted)
            }
        }
    }
}

@Composable
private fun PresentPaywallButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Present Paywall")
    }
}

@Composable
private fun OfferingMetadataDialog(
    offering: Offering,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offering Details") },
        text = {
            OfferingMetadataContent(offering = offering)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun OfferingMetadataContent(offering: Offering) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            MetadataRow("Identifier", offering.identifier)
        }
        item {
            MetadataRow("Description", offering.serverDescription)
        }
        item {
            MetadataRow("Has Paywall", if (offering.hasPaywall) "Yes" else "No")
        }
        item {
            Text(
                text = "Packages (${offering.availablePackages.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(offering.availablePackages) { packageItem ->
            PackageMetadataItem(packageItem = packageItem)
        }
        if (offering.metadata.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Metadata",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(offering.metadata.keys.sorted()) { key ->
                MetadataRow(key, offering.metadata[key]?.toString() ?: "null")
            }
        }
    }
}

@Composable
private fun PackageMetadataItem(packageItem: Package) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Text(
            text = packageItem.product.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Text(
            text = packageItem.identifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Text(
            text = packageItem.product.price.formatted,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PurchaseResultDialog(
    result: PurchaseResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (result) {
                    is PurchaseResult.Success -> "Purchase Successful"
                    is PurchaseResult.SuccessCustomImplementation -> "Purchase Successful"
                    is PurchaseResult.Cancelled -> "Purchase Cancelled"
                    is PurchaseResult.Error -> "Purchase Failed"
                },
            )
        },
        text = {
            Text(
                when (result) {
                    is PurchaseResult.Success -> {
                        if (result.customerInfo != null) {
                            val activeEntitlements =
                                result.customerInfo.entitlements.active.keys.joinToString(", ")
                            "Active Entitlements: ${activeEntitlements.ifEmpty { "None" }}"
                        } else {
                            "Purchase completed successfully."
                        }
                    }
                    is PurchaseResult.SuccessCustomImplementation ->
                        "Purchase completed via your custom BillingClient implementation. " +
                            "The CustomerInfo should be automatically updated in the background with updated entitlements."
                    is PurchaseResult.Cancelled -> "The purchase was cancelled."
                    is PurchaseResult.Error -> result.message
                },
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
