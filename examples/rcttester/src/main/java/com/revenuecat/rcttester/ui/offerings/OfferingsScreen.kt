package com.revenuecat.rcttester.ui.offerings

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingsScreen(
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
    var refreshKey by remember { mutableStateOf(0) }
    var purchasingPackageId by remember { mutableStateOf<String?>(null) }
    var purchaseResult by remember { mutableStateOf<PurchaseResult?>(null) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        error = null
        if (!Purchases.isConfigured) {
            error = "SDK not configured"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            offerings = Purchases.sharedInstance.awaitOfferings()
        } catch (e: PurchasesException) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offerings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Failed to load offerings",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = {
                            if (Purchases.isConfigured) {
                                refreshKey++
                            } else {
                                error = "SDK not configured"
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                offerings != null -> {
                    if (offerings!!.all.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
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
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Move current offering to top if it exists
                            val offeringsList = offerings!!.all.values.toList()
                            val currentOffering = offerings!!.current
                            val sortedOfferings = if (currentOffering != null && offeringsList.firstOrNull()?.identifier != currentOffering.identifier) {
                                listOf(currentOffering) + offeringsList.filter { it.identifier != currentOffering.identifier }
                            } else {
                                offeringsList
                            }

                            items(sortedOfferings) { offering ->
                                OfferingCard(
                                    offering = offering,
                                    isCurrent = offering.identifier == currentOffering?.identifier,
                                    onShowMetadata = { selectedOfferingForMetadata = offering },
                                    onPresentPaywall = { selectedOfferingForPaywall = offering },
                                    onPurchasePackage = { packageItem ->
                                        purchasingPackageId = packageItem.identifier
                                        coroutineScope.launch {
                                            try {
                                                val purchaseParams = PurchaseParams.Builder(activity, packageItem).build()
                                                val result = Purchases.sharedInstance.awaitPurchase(purchaseParams)
                                                purchaseResult = PurchaseResult.Success(
                                                    orderId = result.storeTransaction.orderId ?: "Unknown",
                                                    customerInfo = result.customerInfo
                                                )
                                            } catch (e: PurchasesTransactionException) {
                                                purchaseResult = if (e.userCancelled) {
                                                    PurchaseResult.Cancelled
                                                } else {
                                                    PurchaseResult.Error(e.message ?: "Unknown error", e.code)
                                                }
                                            } catch (e: Exception) {
                                                purchaseResult = PurchaseResult.Error(e.message ?: "Unknown error", null)
                                            } finally {
                                                purchasingPackageId = null
                                            }
                                        }
                                    },
                                    purchasingPackageId = purchasingPackageId,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Metadata Dialog
    selectedOfferingForMetadata?.let { offering ->
        OfferingMetadataDialog(
            offering = offering,
            onDismiss = { selectedOfferingForMetadata = null },
        )
    }
    
    // Purchase Result Dialog
    purchaseResult?.let { result ->
        PurchaseResultDialog(
            result = result,
            onDismiss = { purchaseResult = null },
        )
    }
    
    // Paywall Dialog
    selectedOfferingForPaywall?.let { offering ->
        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setDismissRequest { selectedOfferingForPaywall = null }
                .setOffering(offering)
                .build(),
        )
    }
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
    data class Success(val orderId: String, val customerInfo: com.revenuecat.purchases.CustomerInfo) : PurchaseResult()
    object Cancelled : PurchaseResult()
    data class Error(val message: String, val code: PurchasesErrorCode?) : PurchaseResult()
}

@Composable
private fun OfferingCard(
    offering: Offering,
    isCurrent: Boolean,
    onShowMetadata: () -> Unit,
    onPresentPaywall: () -> Unit,
    onPurchasePackage: (Package) -> Unit,
    purchasingPackageId: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

            if (isCurrent) {
                Text(
                    text = "CURRENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Packages
            if (offering.availablePackages.isNotEmpty()) {
                Text(
                    text = "Packages:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                offering.availablePackages.forEach { packageItem ->
                    val isThisPackagePurchasing = purchasingPackageId == packageItem.identifier
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
                            onClick = { onPurchasePackage(packageItem) },
                            enabled = purchasingPackageId == null,
                        ) {
                            if (isThisPackagePurchasing) {
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
            } else {
                Text(
                    text = "No packages available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Present Paywall button (if offering has a paywall)
            if (offering.hasPaywall) {
                Button(
                    onClick = onPresentPaywall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Present Paywall")
                }
            }
        }
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
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
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
                    is PurchaseResult.Cancelled -> "Purchase Cancelled"
                    is PurchaseResult.Error -> "Purchase Failed"
                }
            )
        },
        text = {
            Text(
                when (result) {
                    is PurchaseResult.Success -> {
                        val activeEntitlements = result.customerInfo.entitlements.active.keys.joinToString(", ")
                        "Order ID: ${result.orderId}\n\nActive Entitlements: ${if (activeEntitlements.isEmpty()) "None" else activeEntitlements}"
                    }
                    is PurchaseResult.Cancelled -> "The purchase was cancelled."
                    is PurchaseResult.Error -> result.message
                }
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
